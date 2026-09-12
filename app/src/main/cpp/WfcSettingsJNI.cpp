#include <jni.h>
#include <array>
#include <cstdio>
#include <cstring>
#include <string>
#include "Platform.h"
#include "SPI_Firmware.h"

// Reads and writes the DS/DSi firmware's "wfcsettings.bin" blob (the three Wi-fi access point
// slots) directly, independently of a running emulator instance, so the settings screen can show
// and edit them even when no game is loaded.
//
// The file layout mirrors what EmulatorArgsBuilder.cpp (generateFirmware) reads/writes when
// booting with generated firmware: the three DSi ExtendedWifiAccessPoint blocks first, then the
// three plain WifiAccessPoint blocks (see EmulatorArgsBuilder.cpp, TOTAL_WFC_SETTINGS_SIZE).

using namespace melonDS;
using namespace melonDS::Platform;

namespace
{
    constexpr unsigned WFC_SLOT_COUNT = 3;

    struct WfcSlots
    {
        std::array<Firmware::ExtendedWifiAccessPoint, WFC_SLOT_COUNT> extended;
        std::array<Firmware::WifiAccessPoint, WFC_SLOT_COUNT> basic;
    };

    void initDefaultWfcSlots(WfcSlots& slots)
    {
        slots.extended = {
            Firmware::ExtendedWifiAccessPoint(),
            Firmware::ExtendedWifiAccessPoint(),
            Firmware::ExtendedWifiAccessPoint(),
        };
        slots.basic = {
            Firmware::WifiAccessPoint(),
            Firmware::WifiAccessPoint(),
            Firmware::WifiAccessPoint(),
        };
    }

    bool loadWfcSlots(const std::string& path, WfcSlots& slots)
    {
        FileHandle* f = OpenFile(path, FileMode::Read);
        if (!f)
            return false;

        bool ok = FileRead(slots.extended.data(), sizeof(slots.extended), 1, f) == 1
                && FileRead(slots.basic.data(), sizeof(slots.basic), 1, f) == 1;
        CloseFile(f);
        return ok;
    }

    bool saveWfcSlots(const std::string& path, const WfcSlots& slots)
    {
        FileHandle* f = OpenFile(path, FileMode::Write);
        if (!f)
            return false;

        bool ok = FileWrite(slots.extended.data(), sizeof(slots.extended), 1, f) == 1
               && FileWrite(slots.basic.data(), sizeof(slots.basic), 1, f) == 1;
        CloseFile(f);
        return ok;
    }

    std::string ipToString(const IpAddress& ip)
    {
        char buffer[16];
        snprintf(buffer, sizeof(buffer), "%u.%u.%u.%u", ip[0], ip[1], ip[2], ip[3]);
        return buffer;
    }

    // SSID is a fixed 32-byte field and isn't guaranteed to be null-terminated when full.
    std::string ssidToString(const Firmware::WifiAccessPoint& ap)
    {
        size_t length = strnlen(ap.SSID, sizeof(ap.SSID));
        return std::string(ap.SSID, length);
    }

    std::string serializeSlot(const Firmware::WifiAccessPoint& ap)
    {
        bool enabled = ap.Status != Firmware::AccessPointStatus::NotConfigured;
        return std::string(enabled ? "1" : "0") + ";" + ssidToString(ap) + ";" + ipToString(ap.PrimaryDns) + ";" + ipToString(ap.SecondaryDns);
    }

    // Accepts a plain "a.b.c.d" IPv4 address, each component 0-255, with nothing trailing.
    bool parseIpv4(const std::string& text, IpAddress& out)
    {
        unsigned a, b, c, d;
        char extra;
        if (sscanf(text.c_str(), "%u.%u.%u.%u%c", &a, &b, &c, &d, &extra) != 4)
            return false;
        if (a > 255 || b > 255 || c > 255 || d > 255)
            return false;

        out = {(u8) a, (u8) b, (u8) c, (u8) d};
        return true;
    }

    std::string jstringToStdString(JNIEnv* env, jstring string)
    {
        const char* chars = env->GetStringUTFChars(string, nullptr);
        std::string result(chars);
        env->ReleaseStringUTFChars(string, chars);
        return result;
    }
}

extern "C"
{

JNIEXPORT jobjectArray JNICALL
Java_me_magnum_melonds_common_WfcSettings_readSlots(JNIEnv* env, jobject thiz, jstring pathString)
{
    std::string path = jstringToStdString(env, pathString);

    WfcSlots slots;
    if (!loadWfcSlots(path, slots))
        return nullptr;

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize) WFC_SLOT_COUNT, stringClass, nullptr);
    for (unsigned i = 0; i < WFC_SLOT_COUNT; i++)
    {
        env->SetObjectArrayElement(result, (jsize) i, env->NewStringUTF(serializeSlot(slots.basic[i]).c_str()));
    }

    return result;
}

JNIEXPORT jboolean JNICALL
Java_me_magnum_melonds_common_WfcSettings_writeSlot(JNIEnv* env, jobject thiz, jstring pathString, jint slot, jboolean enabled, jstring primaryDnsString, jstring secondaryDnsString)
{
    if (slot < 0 || slot >= (jint) WFC_SLOT_COUNT)
        return JNI_FALSE;

    std::string path = jstringToStdString(env, pathString);
    std::string primaryDnsText = jstringToStdString(env, primaryDnsString);
    std::string secondaryDnsText = jstringToStdString(env, secondaryDnsString);

    IpAddress primaryDns {};
    IpAddress secondaryDns {};
    if (!parseIpv4(primaryDnsText, primaryDns) || !parseIpv4(secondaryDnsText, secondaryDns))
        return JNI_FALSE;

    WfcSlots slots;
    if (!loadWfcSlots(path, slots))
        initDefaultWfcSlots(slots);

    Firmware::WifiAccessPoint& ap = slots.basic[slot];
    ap.Status = enabled ? Firmware::AccessPointStatus::Normal : Firmware::AccessPointStatus::NotConfigured;
    ap.PrimaryDns = primaryDns;
    ap.SecondaryDns = secondaryDns;
    ap.UpdateChecksum();

    // The DSi extended block embeds its own copy of the base access point (Data.Base); keep it
    // in sync so the slot behaves the same whether the game boots in DS or DSi mode.
    slots.extended[slot].Data.Base = ap;
    slots.extended[slot].UpdateChecksum();

    return saveWfcSlots(path, slots) ? JNI_TRUE : JNI_FALSE;
}

}
