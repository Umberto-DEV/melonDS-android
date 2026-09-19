#include <jni.h>
#include <array>
#include <cstdio>
#include <cstring>
#include <optional>
#include <string>
#include "MelonDS.h"
#include "MelonInstance.h"
#include "Platform.h"
#include "SPI_Firmware.h"

// Reads and writes the three Wi-fi access point slots of whichever firmware the console will
// actually use, independently of a running emulator instance, so the settings screen can show and
// edit them even when no game is loaded:
//
//  - built-in firmware in DS mode: the slots live in "wfcsettings.bin" in the app's internal
//    files dir. Its layout mirrors what generateFirmware() in EmulatorArgsBuilder.cpp reads and
//    writes: the three DSi ExtendedWifiAccessPoint blocks first, then the three plain
//    WifiAccessPoint blocks (TOTAL_WFC_SETTINGS_SIZE there).
//  - imported firmware (DS or DSi, any of the 128/256/512 KB sizes): the slots live inside the
//    user's firmware file, at the offsets the firmware header itself declares. The Firmware class
//    does that arithmetic (Firmware::GetWifiAccessPointOffset()), so there are no hardcoded
//    offsets here.
//
// The Kotlin side decides which of the two is in play, with the same conditions the emulator uses
// when it builds its SaveManager (MelonInstance.cpp, constructor).
//
// While a session is running the file is NOT the source of truth: the core serves the firmware
// from its own in-memory buffer and its SaveManager would overwrite the file on the next flush.
// The live entry points at the bottom go through the running instance instead.

using namespace melonDS;
using namespace melonDS::Platform;
using MelonDSAndroid::WfcSlotData;

namespace
{
    constexpr unsigned WFC_SLOT_COUNT = 3;

    // The three sizes a DS/DSi firmware image can have (Firmware::FixFirmwareLength()). Anything
    // else is either not a firmware or a truncated one, and is refused rather than "fixed": we'd
    // be writing a padded buffer back over the user's file.
    bool isFirmwareLengthValid(u64 length)
    {
        return length == 0x20000 || length == 0x40000 || length == 0x80000;
    }

    struct WfcSettingsFileContents
    {
        std::array<Firmware::ExtendedWifiAccessPoint, WFC_SLOT_COUNT> extended;
        std::array<Firmware::WifiAccessPoint, WFC_SLOT_COUNT> basic;
    };

    void initDefaultWfcSlots(WfcSettingsFileContents& slots)
    {
        slots.extended = {
            Firmware::ExtendedWifiAccessPoint(),
            Firmware::ExtendedWifiAccessPoint(),
            Firmware::ExtendedWifiAccessPoint(),
        };
        slots.basic = {
            // Slot 1 is the emulated access point (SSID melonAP, Normal), exactly as
            // generateFirmware() builds it in EmulatorArgsBuilder.cpp; leaving it unconfigured
            // would wipe it on the first visit to the settings screen. The console type is not
            // known here, so use the DS one: it only changes the MTU stored in the slot.
            Firmware::WifiAccessPoint(0),
            Firmware::WifiAccessPoint(),
            Firmware::WifiAccessPoint(),
        };
    }

    bool loadWfcSettingsFile(const std::string& path, WfcSettingsFileContents& slots)
    {
        FileHandle* f = OpenFile(path, FileMode::Read);
        if (!f)
            return false;

        bool ok = FileRead(slots.extended.data(), sizeof(slots.extended), 1, f) == 1
                && FileRead(slots.basic.data(), sizeof(slots.basic), 1, f) == 1;
        CloseFile(f);
        return ok;
    }

    bool saveWfcSettingsFile(const std::string& path, const WfcSettingsFileContents& slots)
    {
        FileHandle* f = OpenFile(path, FileMode::Write);
        if (!f)
            return false;

        bool ok = FileWrite(slots.extended.data(), sizeof(slots.extended), 1, f) == 1
               && FileWrite(slots.basic.data(), sizeof(slots.basic), 1, f) == 1;
        CloseFile(f);
        return ok;
    }

    // Loads the user's firmware file into a Firmware object, which is what knows where the access
    // points are for each firmware revision and size. Returns nothing when the file can't be
    // read, isn't a plausible firmware, or declares a user settings offset that would put the
    // access points outside the image.
    std::optional<Firmware> loadFirmwareFile(const std::string& path)
    {
        FileHandle* f = OpenFile(path, FileMode::Read);
        if (!f)
            return std::nullopt;

        u64 length = FileLength(f);
        if (!isFirmwareLengthValid(length))
        {
            Log(LogLevel::Error, "WFC: \"%s\" is %llu bytes, not a firmware image\n", path.c_str(), (unsigned long long) length);
            CloseFile(f);
            return std::nullopt;
        }

        Firmware firmware(f);
        CloseFile(f);

        if (!firmware.Buffer() || firmware.Length() != length)
            return std::nullopt;

        if (!MelonDSAndroid::wfcAccessPointsInBounds(firmware))
        {
            Log(LogLevel::Error, "WFC: \"%s\" has a bogus user settings offset (%u)\n", path.c_str(), firmware.GetUserDataOffset());
            return std::nullopt;
        }

        return firmware;
    }

    bool saveFirmwareFile(const std::string& path, const Firmware& firmware)
    {
        // Preserve keeps the open from truncating, so a failure half way through leaves a
        // full-length firmware holding a mix of old and new bytes (which still has valid
        // checksums for every region we didn't touch) instead of an empty file. The backup the
        // Kotlin side takes before the first write is the real safety net.
        const FileMode preservingMode = static_cast<FileMode>(FileMode::Read | FileMode::Write | FileMode::Preserve);
        FileHandle* f = OpenFile(path, preservingMode);
        if (!f)
            f = OpenFile(path, FileMode::Write);
        if (!f)
            return false;

        FileRewind(f);
        bool ok = FileWrite(firmware.Buffer(), firmware.Length(), 1, f) == 1;
        ok = CloseFile(f) && ok;
        return ok;
    }

    std::string ipToString(const u8 ip[4])
    {
        char buffer[16];
        snprintf(buffer, sizeof(buffer), "%u.%u.%u.%u", ip[0], ip[1], ip[2], ip[3]);
        return buffer;
    }

    // "enabled;name;primaryDns;secondaryDns". The name is the slot's SSID as stored in the
    // firmware; the free label the user types is kept by the app, see writeSlot() below.
    std::string serializeSlot(const WfcSlotData& slot)
    {
        return std::string(slot.enabled ? "1" : "0") + ";" + std::string(slot.name) + ";"
             + ipToString(slot.primaryDns) + ";" + ipToString(slot.secondaryDns);
    }

    jobjectArray serializeSlots(JNIEnv* env, const WfcSlotData slots[WFC_SLOT_COUNT])
    {
        jclass stringClass = env->FindClass("java/lang/String");
        jobjectArray result = env->NewObjectArray((jsize) WFC_SLOT_COUNT, stringClass, nullptr);
        for (unsigned i = 0; i < WFC_SLOT_COUNT; i++)
        {
            env->SetObjectArrayElement(result, (jsize) i, env->NewStringUTF(serializeSlot(slots[i]).c_str()));
        }

        return result;
    }

    // Accepts a plain "a.b.c.d" IPv4 address, each component 0-255, with nothing trailing.
    bool parseIpv4(const std::string& text, u8 out[4])
    {
        unsigned a, b, c, d;
        char extra;
        if (sscanf(text.c_str(), "%u.%u.%u.%u%c", &a, &b, &c, &d, &extra) != 4)
            return false;
        if (a > 255 || b > 255 || c > 255 || d > 255)
            return false;

        out[0] = (u8) a;
        out[1] = (u8) b;
        out[2] = (u8) c;
        out[3] = (u8) d;
        return true;
    }

    std::string jstringToStdString(JNIEnv* env, jstring string)
    {
        if (!string)
            return {};

        const char* chars = env->GetStringUTFChars(string, nullptr);
        std::string result(chars);
        env->ReleaseStringUTFChars(string, chars);
        return result;
    }

    // Fills in the slot the caller asked for. An empty name leaves the stored SSID alone, except
    // on a slot that was never configured: the console looks the network up by name, so it gets
    // the emulated access point's one, as the generated slot 1 already has.
    bool buildSlotData(
        JNIEnv* env,
        jboolean enabled,
        jstring nameString,
        jstring primaryDnsString,
        jstring secondaryDnsString,
        const char* currentSsid,
        WfcSlotData& slotData
    ) {
        std::string name = jstringToStdString(env, nameString);
        if (name.length() > 32)
            return false;

        if (!parseIpv4(jstringToStdString(env, primaryDnsString), slotData.primaryDns)
            || !parseIpv4(jstringToStdString(env, secondaryDnsString), slotData.secondaryDns))
            return false;

        slotData.enabled = enabled;
        memset(slotData.name, 0, sizeof(slotData.name));
        if (!name.empty())
            memcpy(slotData.name, name.c_str(), name.length());
        else if (enabled && strnlen(currentSsid, 32) == 0)
            strncpy(slotData.name, melonDS::DEFAULT_SSID, sizeof(slotData.name) - 1);

        return true;
    }
}

extern "C"
{

JNIEXPORT jobjectArray JNICALL
Java_me_magnum_melonds_common_WfcSettings_readSlots(JNIEnv* env, jobject thiz, jstring pathString, jboolean isFirmwareFile)
{
    std::string path = jstringToStdString(env, pathString);
    WfcSlotData slots[WFC_SLOT_COUNT];

    if (isFirmwareFile)
    {
        std::optional<Firmware> firmware = loadFirmwareFile(path);
        if (!firmware)
            return nullptr;

        const auto& accessPoints = firmware->GetAccessPoints();
        for (unsigned i = 0; i < WFC_SLOT_COUNT; i++)
            MelonDSAndroid::extractWfcSlotData(accessPoints[i], slots[i]);
    }
    else
    {
        WfcSettingsFileContents contents;
        if (!loadWfcSettingsFile(path, contents))
            return nullptr;

        for (unsigned i = 0; i < WFC_SLOT_COUNT; i++)
            MelonDSAndroid::extractWfcSlotData(contents.basic[i], slots[i]);
    }

    return serializeSlots(env, slots);
}

JNIEXPORT jboolean JNICALL
Java_me_magnum_melonds_common_WfcSettings_writeSlot(
    JNIEnv* env,
    jobject thiz,
    jstring pathString,
    jboolean isFirmwareFile,
    jint slot,
    jboolean enabled,
    jstring nameString,
    jstring primaryDnsString,
    jstring secondaryDnsString
) {
    if (slot < 0 || slot >= (jint) WFC_SLOT_COUNT)
        return JNI_FALSE;

    std::string path = jstringToStdString(env, pathString);
    WfcSlotData slotData {};

    if (isFirmwareFile)
    {
        std::optional<Firmware> firmware = loadFirmwareFile(path);
        if (!firmware)
            return JNI_FALSE;

        Firmware::WifiAccessPoint& accessPoint = firmware->GetAccessPoints()[slot];
        if (!buildSlotData(env, enabled, nameString, primaryDnsString, secondaryDnsString, accessPoint.SSID, slotData))
            return JNI_FALSE;

        MelonDSAndroid::applyWfcSlotData(accessPoint, slotData);

        // Only DSi firmware owns the extended blocks; in a DS image that area holds unrelated
        // firmware data and is left untouched.
        if (firmware->GetHeader().ConsoleType == Firmware::FirmwareConsoleType::DSi)
        {
            Firmware::ExtendedWifiAccessPoint& extendedAccessPoint = firmware->GetExtendedAccessPoints()[slot];
            extendedAccessPoint.Data.Base = accessPoint;
            extendedAccessPoint.UpdateChecksum();
        }

        return saveFirmwareFile(path, *firmware) ? JNI_TRUE : JNI_FALSE;
    }

    WfcSettingsFileContents contents;
    if (!loadWfcSettingsFile(path, contents))
        initDefaultWfcSlots(contents);

    Firmware::WifiAccessPoint& accessPoint = contents.basic[slot];
    if (!buildSlotData(env, enabled, nameString, primaryDnsString, secondaryDnsString, accessPoint.SSID, slotData))
        return JNI_FALSE;

    MelonDSAndroid::applyWfcSlotData(accessPoint, slotData);

    // The DSi extended block embeds its own copy of the base access point (Data.Base); keep it
    // in sync so the slot behaves the same whether the game boots in DS or DSi mode. Unlike a
    // real firmware image, this file is ours and its extended region holds nothing else.
    contents.extended[slot].Data.Base = accessPoint;
    contents.extended[slot].UpdateChecksum();

    return saveWfcSettingsFile(path, contents) ? JNI_TRUE : JNI_FALSE;
}

// Reads the slots out of the firmware the running instance is serving over SPI. Returns null when
// no session is running, so the caller can fall back to the file.
JNIEXPORT jobjectArray JNICALL
Java_me_magnum_melonds_common_WfcSettings_readLiveSlots(JNIEnv* env, jobject thiz)
{
    WfcSlotData slots[WFC_SLOT_COUNT];

    bool wasPaused = MelonDSAndroid::pauseEmulationThreadForSyncOperation();
    bool read = MelonDSAndroid::readRunningInstanceWfcSlots(slots);
    MelonDSAndroid::resumeEmulationThreadAfterSyncOperation(wasPaused);

    if (!read)
        return nullptr;

    return serializeSlots(env, slots);
}

// Writes one slot into the live firmware buffer and asks for the region to be flushed to the
// firmware's backing file. The console re-reads the slots from the firmware SPI every time it
// opens a connection (nothing is cached in RAM at boot: SPI.cpp, FirmwareMem::Write, command
// 0x03), so this takes effect on the next connection attempt, without a reset.
JNIEXPORT jboolean JNICALL
Java_me_magnum_melonds_common_WfcSettings_writeLiveSlot(
    JNIEnv* env,
    jobject thiz,
    jint slot,
    jboolean enabled,
    jstring nameString,
    jstring primaryDnsString,
    jstring secondaryDnsString
) {
    if (slot < 0 || slot >= (jint) WFC_SLOT_COUNT)
        return JNI_FALSE;

    // Modifying the firmware buffer (and requesting the save flush) while the emulator thread
    // runs would race the console's own SPI writes and the SaveManager staging done at the end of
    // every frame. The read happens in the same window: it is what tells us whether the slot
    // already has an SSID worth keeping.
    bool wasPaused = MelonDSAndroid::pauseEmulationThreadForSyncOperation();

    bool written = false;
    WfcSlotData currentSlots[WFC_SLOT_COUNT];
    if (MelonDSAndroid::readRunningInstanceWfcSlots(currentSlots))
    {
        WfcSlotData slotData {};
        if (buildSlotData(env, enabled, nameString, primaryDnsString, secondaryDnsString, currentSlots[slot].name, slotData))
            written = MelonDSAndroid::writeRunningInstanceWfcSlot(slot, slotData);
    }

    MelonDSAndroid::resumeEmulationThreadAfterSyncOperation(wasPaused);

    return written ? JNI_TRUE : JNI_FALSE;
}

}
