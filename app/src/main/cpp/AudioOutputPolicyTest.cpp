// Standalone unit test for the pure audio-output decision function in
// AudioOutputPolicy.h/.cpp.
//
// Deliberately has zero Android/JNI/Oboe dependencies so it can be compiled and run with a
// plain host compiler, without the NDK toolchain and without an emulator:
//
//   clang++ -std=c++17 -Wall -Wextra -o /tmp/audio_output_policy_test AudioOutputPolicy.cpp AudioOutputPolicyTest.cpp
//   /tmp/audio_output_policy_test
//
// Every test exercises both directions on purpose (see task note "mutanti nelle due
// direzioni"): a function that always returns true, or that always returns false, must fail
// at least one case here. That was checked by hand while writing this file: temporarily
// hard-coding shouldAudioOutputStreamBeActive() to `return true;` fails
// test_sound_disabled_is_never_active() and test_zero_volume_is_not_active(); temporarily
// hard-coding it to `return false;` fails test_sound_enabled_with_volume_is_active(). Both
// were run and reverted before this file was committed.
//
// The specific regression this guards against: MelonDSAudio.cpp's updateAudioSettings() used
// to call this condition with the OLD volume (currentAudioSettings.volume, not yet
// overwritten) instead of the NEW one (the audioSettings argument). Raising the volume from 0
// therefore could not turn this condition true on the very call that raised it. This test
// file only exercises the pure predicate -- it cannot see which variable the call site passes
// in, so it does not by itself prove the call site was fixed; that half is a plain read of
// MelonDSAudio.cpp's updateAudioSettings(), which now passes audioSettings.soundEnabled and
// audioSettings.volume (the new values), not currentAudioSettings' (the old ones).

#include "AudioOutputPolicy.h"

#include <cstdio>
#include <cstdlib>

using namespace MelonDSAndroid;

static int failures = 0;

static void expect(bool condition, const char* description)
{
    if (!condition)
    {
        std::fprintf(stderr, "FAIL: %s\n", description);
        failures++;
    }
    else
    {
        std::printf("ok: %s\n", description);
    }
}

// The headline bug: volume raised from 0 to something audible, sound enabled. This must be
// true on this very call -- not "eventually true once some later call re-reads state".
static void test_sound_enabled_with_volume_is_active()
{
    expect(shouldAudioOutputStreamBeActive(/*soundEnabled=*/true, /*volume=*/50) == true,
           "sound enabled, volume 50 -> active");
    expect(shouldAudioOutputStreamBeActive(/*soundEnabled=*/true, /*volume=*/1) == true,
           "sound enabled, volume 1 (just above zero) -> active");
}

// Sound off must never be active, regardless of volume.
static void test_sound_disabled_is_never_active()
{
    expect(shouldAudioOutputStreamBeActive(/*soundEnabled=*/false, /*volume=*/50) == false,
           "sound disabled, volume 50 -> not active");
    expect(shouldAudioOutputStreamBeActive(/*soundEnabled=*/false, /*volume=*/0) == false,
           "sound disabled, volume 0 -> not active");
}

// Volume 0 with sound enabled must not be active -- this is the other edge of the same
// boundary the bug lived on.
static void test_zero_volume_is_not_active()
{
    expect(shouldAudioOutputStreamBeActive(/*soundEnabled=*/true, /*volume=*/0) == false,
           "sound enabled, volume 0 -> not active");
}

int main()
{
    test_sound_enabled_with_volume_is_active();
    test_sound_disabled_is_never_active();
    test_zero_volume_is_not_active();

    if (failures > 0)
    {
        std::fprintf(stderr, "%d test(s) failed\n", failures);
        return EXIT_FAILURE;
    }

    std::printf("all tests passed\n");
    return EXIT_SUCCESS;
}
