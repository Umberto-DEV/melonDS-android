#ifndef MELONDS_ANDROID_JITTESTSUPPORT_H
#define MELONDS_ANDROID_JITTESTSUPPORT_H

/*
 * Shared scaffolding for the two core-dependent host tests (JitDifferentialTest and
 * JitInvalidationTest): the pass/fail bookkeeping and a stable content hash.
 */

#include <cstddef>
#include <cstdint>
#include <cstdio>

namespace melonDS::Platform { extern bool VerboseLog; }

namespace JitTest
{

inline int Failures = 0;

inline void Expect(bool condition, const char* description)
{
    printf("  %s %s\n", condition ? "ok  " : "FAIL", description);
    if (!condition)
        Failures++;
}

/// FNV-1a. Not cryptographic; it only has to notice that two framebuffers differ, and to give
/// the same answer on every build so a hash can be compared across runs by eye.
inline uint64_t Hash(const void* data, size_t length)
{
    const auto* bytes = static_cast<const uint8_t*>(data);
    uint64_t hash = 1469598103934665603ULL;
    for (size_t i = 0; i < length; i++)
    {
        hash ^= bytes[i];
        hash *= 1099511628211ULL;
    }
    return hash;
}

}

#endif //MELONDS_ANDROID_JITTESTSUPPORT_H
