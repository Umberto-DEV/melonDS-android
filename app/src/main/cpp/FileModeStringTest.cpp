// Standalone unit test for the FileMode -> mode string translation in FileModeString.h/.cpp,
// shared by the direct-path back-end (PlatformAndroid.cpp) and the SAF back-end
// (UriFileHandler.cpp).
//
// Zero Android/JNI/Oboe dependencies on purpose, so it builds and runs with a plain host
// compiler (see tools/host-tests.sh):
//
//   clang++ -std=c++17 -I<melonDS-android-lib/src> -o /tmp/file_mode_string_test \
//       FileModeString.cpp FileModeStringTest.cpp
//
// What it is here to protect: SaveManager asks for Read|Write|Preserve when it writes a save
// file, and the whole point of that request is that NEITHER back-end may truncate the
// destination at open time. If someone ever "simplifies" the table below and the SAF string
// goes back to "wt", a process kill during a flush empties the player's save file again
// (upstream #1594, #1531) and nothing else in the build would notice.

#include "FileModeString.h"

#include <cstdio>
#include <cstdlib>
#include <string>

using namespace MelonDSAndroid;
using melonDS::Platform::FileMode;

static int failures = 0;

static void expectEqual(const std::string& actual, const std::string& expected, const char* description)
{
    if (actual != expected)
    {
        std::fprintf(stderr, "FAIL: %s (expected \"%s\", got \"%s\")\n", description, expected.c_str(), actual.c_str());
        failures++;
    }
}

static constexpr FileMode mode(unsigned flags)
{
    return static_cast<FileMode>(flags);
}

// The mode SaveManager::FlushSecondaryBuffer() now uses.
static constexpr FileMode PreservingWrite = mode(FileMode::Read | FileMode::Write | FileMode::Preserve);

static void test_preserving_write_never_truncates()
{
    // "r+b": opens the existing file for read/write with the contents intact.
    expectEqual(GetStdioModeString(PreservingWrite, true), "r+b", "stdio, preserving write, file exists");
    // "rw" is the only SAF mode that opens an existing document for writing without truncating.
    expectEqual(GetSafModeString(PreservingWrite, true), "rw", "SAF, preserving write, file exists");
    expectEqual(GetSafModeString(PreservingWrite, false), "rw", "SAF, preserving write, file missing");
}

static void test_preserving_write_still_creates_a_missing_file()
{
    // Nothing to preserve, so the file has to be created: "w+b" both creates and allows reads.
    expectEqual(GetStdioModeString(PreservingWrite, false), "w+b", "stdio, preserving write, file missing");
}

static void test_plain_write_is_the_truncating_one()
{
    // This is what the flush used to ask for, and why a kill mid-write left an empty save.
    expectEqual(GetStdioModeString(FileMode::Write, true), "wb", "stdio, plain write");
    expectEqual(GetSafModeString(FileMode::Write, true), "wt", "SAF, plain write (t = truncate)");
}

static void test_read_and_append_are_unchanged()
{
    expectEqual(GetStdioModeString(FileMode::Read, true), "rb", "stdio, read only");
    expectEqual(GetSafModeString(FileMode::Read, true), "r", "SAF, read only");
    expectEqual(GetStdioModeString(FileMode::Append, true), "ab", "stdio, append");
    expectEqual(GetSafModeString(FileMode::Append, true), "wa", "SAF, append");
    expectEqual(GetStdioModeString(FileMode::ReadWriteExisting, true), "r+b", "stdio, read/write existing");
    expectEqual(GetStdioModeString(FileMode::ReadText, true), "r", "stdio, read text");
}

int main()
{
    test_preserving_write_never_truncates();
    test_preserving_write_still_creates_a_missing_file();
    test_plain_write_is_the_truncating_one();
    test_read_and_append_are_unchanged();

    if (failures != 0)
    {
        std::fprintf(stderr, "%d check(s) failed\n", failures);
        return EXIT_FAILURE;
    }

    std::printf("FileModeString: all checks passed\n");
    return EXIT_SUCCESS;
}
