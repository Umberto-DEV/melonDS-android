#ifndef WFC_SAVE_RANGE_H
#define WFC_SAVE_RANGE_H

#include "types.h"

namespace MelonDSAndroid
{
struct WfcSaveRange
{
    melonDS::u32 offset;
    melonDS::u32 length;
};

inline WfcSaveRange wfcSaveRange(melonDS::u32 basicOffset, melonDS::u32 basicLength,
                                melonDS::u32 extendedOffset, bool includeExtended)
{
    // Extended APs precede the basic APs. SaveManager only copies this dirty range
    // after its initial full-buffer snapshot, so both copies must be included.
    const auto start = includeExtended ? extendedOffset : basicOffset;
    return {start, basicOffset + basicLength - start};
}
}

#endif
