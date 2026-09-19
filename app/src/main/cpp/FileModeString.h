#ifndef MELONDS_ANDROID_FILEMODESTRING_H
#define MELONDS_ANDROID_FILEMODESTRING_H

#include <string>

#include <Platform.h>

namespace MelonDSAndroid
{

/**
 * Translates a melonDS FileMode into the mode string for fopen()/fdopen().
 *
 * Extracted from PlatformAndroid.cpp (where it was a set of file-static helpers) and from
 * UriFileHandler.cpp (where the same decision table existed a second time) so that the two
 * back-ends cannot drift apart, and so the translation can be exercised by a host test with
 * no NDK, no JNI and no emulator. See FileModeStringTest.cpp.
 *
 * @param fileExists whether the file is already there; only Preserve looks at it.
 */
std::string GetStdioModeString(melonDS::Platform::FileMode mode, bool fileExists);

/**
 * Translates a melonDS FileMode into the mode string that
 * ContentResolver.openFileDescriptor() understands ("r", "rw", "wt", "wa"...).
 *
 * The important case is Read|Write|Preserve, which must come out as "rw": that is the only
 * documented SAF mode that opens an existing document for writing *without* truncating it.
 * Dropping the "t" is what keeps a half-written save file from ending up empty.
 */
std::string GetSafModeString(melonDS::Platform::FileMode mode, bool fileExists);

}

#endif //MELONDS_ANDROID_FILEMODESTRING_H
