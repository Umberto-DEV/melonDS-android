#include "FileModeString.h"

using namespace melonDS::Platform;

namespace MelonDSAndroid
{

static constexpr char AccessMode(FileMode mode, bool fileExists)
{
    if (mode & FileMode::Append)
        return  'a';

    if (!(mode & FileMode::Write))
        // If we're only opening the file for reading...
        return 'r';

    if (mode & (FileMode::NoCreate))
        // If we're not allowed to create a new file...
        return 'r'; // Open in "r+" mode (IsExtended will add the "+")

    if ((mode & FileMode::Preserve) && fileExists)
        // If we're not allowed to overwrite a file that already exists...
        return 'r'; // Open in "r+" mode (IsExtended will add the "+")

    return 'w';
}

static constexpr bool IsExtended(FileMode mode)
{
    // fopen's "+" flag always opens the file for read/write
    return (mode & FileMode::ReadWrite) == FileMode::ReadWrite;
}

std::string GetStdioModeString(FileMode mode, bool fileExists)
{
    std::string modeString;

    modeString += AccessMode(mode, fileExists);

    if (IsExtended(mode))
        modeString += '+';

    if (!(mode & FileMode::Text))
        modeString += 'b';

    return modeString;
}

std::string GetSafModeString(FileMode mode, bool fileExists)
{
    (void) fileExists;

    std::string modeString;

    if (mode & FileMode::Read)
        modeString += 'r';

    if (mode & FileMode::Write)
    {
        modeString += 'w';

        if (mode & FileMode::Append)
            modeString += 'a';
        else if (!(mode & FileMode::Preserve))
            // "t" is the truncating flavour. Preserve is precisely the request not to truncate.
            modeString += 't';
    }
    else if (mode & FileMode::Append)
        modeString += "wa";

    return modeString;
}

}
