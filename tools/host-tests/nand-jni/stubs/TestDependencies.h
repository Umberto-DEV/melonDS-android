#pragma once
#include <array>
#include <cstdint>
#include <cstring>
#include <memory>
#include <string>
#include <vector>
using u8 = std::uint8_t;
using u32 = std::uint32_t;
struct NDSHeader { u32 DSiPublicSavSize = 1024; u32 DSiPrivateSavSize = 2048; int AppFlags = 0; };
struct NDSBanner { std::array<u8,512> Icon{}; std::array<u32,16> Palette{}; char16_t EnglishTitle[128] = u"Test title\nTest producer"; };
namespace melonDS {
namespace Platform {
enum class FileMode { Read, ReadWriteExisting };
enum class FileSeekOrigin { Start };
inline void* OpenFile(const std::string&, FileMode) { return nullptr; }
inline void* OpenLocalFile(const std::string&, FileMode) { return nullptr; }
inline void FileSeek(void*, int, FileSeekOrigin) {}
inline void FileRead(void*, int, int, void*) {}
inline void CloseFile(void*) {}
}
namespace DSi_TMD { struct TitleMetadata {}; }
namespace DSi_NAND {
struct NANDImage { NANDImage(void*, u8*) {} explicit operator bool() const { return true; } };
struct NANDMount {
    std::vector<u32> titles;
    explicit NANDMount(NANDImage&) {}
    void ListTitles(u32, std::vector<u32>& output) { output = titles; }
    void GetTitleInfo(u32, u32, u32&, NDSHeader*, NDSBanner*) {}
    bool TitleExists(u32, u32) { return false; }
    void DeleteTitle(u32, u32) {}
    bool ImportTitle(const char*, DSi_TMD::TitleMetadata&, bool) { return true; }
    bool ImportTitleData(u32,u32,jint,const char*) { return true; }
    bool ExportTitleData(u32,u32,jint,const char*) { return true; }
};
}
}
namespace Platform = melonDS::Platform;
namespace MelonDSAndroid {
struct EmulatorConfiguration { std::string dsiBios7Path; std::string dsiNandPath; };
inline void setConfiguration(EmulatorConfiguration) {}
inline void BuildRomIcon(const std::array<u8,512>&, const std::array<u32,16>&, u32* data) {
    for (int i=0; i<1024; ++i) data[i] = 0x12345678;
}
}
namespace MelonDSAndroidConfiguration {
inline MelonDSAndroid::EmulatorConfiguration buildEmulatorConfiguration(JNIEnv*, jobject) { return {}; }
}
