// Standalone unit test for the pure RTC-sync decision function in RtcSync.h/.cpp.
//
// Deliberately has zero Android/JNI/melonDS dependencies so it can be compiled and run
// with a plain host compiler, without the NDK toolchain and without an emulator:
//
//   clang++ -std=c++17 -Wall -Wextra -o /tmp/rtc_sync_test RtcSync.cpp RtcSyncTest.cpp
//   /tmp/rtc_sync_test
//
// Every test exercises both directions on purpose (see task note "mutanti nelle due
// direzioni"): a function that always advances, or that never advances, must fail at
// least one case here. That was checked by hand while writing this file: temporarily
// hard-coding rtcSyncAction() to `return RtcSyncAction::ADVANCE;` fails
// test_at_or_ahead_of_host_never_advances(); temporarily hard-coding it to
// `return RtcSyncAction::NONE;` fails test_far_behind_host_advances(). Both were run and
// reverted before this file was committed.

#include "RtcSync.h"

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

// The headline bug: the app was backgrounded for four hours (per the diagnosis, pause()/
// resume() never touch the RTC), so the DS clock is four hours behind the host.
static void test_far_behind_host_advances()
{
    const int64_t hostNow = 1000000;
    const int64_t dsClock = hostNow - 4 * 3600;
    expect(rtcSyncAction(dsClock, hostNow) == RtcSyncAction::ADVANCE,
           "DS clock four hours behind host -> ADVANCE");
}

// A one-second-behind clock, at a 1s threshold, should still advance: the option's whole
// point is to catch up a lagging clock.
static void test_just_over_threshold_advances()
{
    const int64_t hostNow = 1000000;
    const int64_t dsClock = hostNow - 1;
    expect(rtcSyncAction(dsClock, hostNow, /*minDriftSeconds=*/1) == RtcSyncAction::ADVANCE,
           "DS clock exactly at the 1s threshold -> ADVANCE");
}

// Below the configured threshold, do nothing -- this is what keeps the RTC from being
// re-stamped over sub-second/rounding differences when the two clocks are already
// effectively in sync.
static void test_under_threshold_does_not_advance()
{
    const int64_t hostNow = 1000000;
    const int64_t dsClock = hostNow - 2;
    expect(rtcSyncAction(dsClock, hostNow, /*minDriftSeconds=*/3) == RtcSyncAction::NONE,
           "DS clock 2s behind with a 3s threshold -> NONE");
}

// The production threshold (kRtcSyncMinDriftSeconds, exported from RtcSync.h so this test
// exercises the exact number that ships): one second under it must not advance, and
// reaching it must advance. Sync now runs on resume rather than every frame, so this only
// needs to absorb whole-second rounding/small drift, not survive per-frame re-evaluation.
static void test_production_threshold_boundary()
{
    const int64_t hostNow = 1000000;

    expect(rtcSyncAction(hostNow - (kRtcSyncMinDriftSeconds - 1), hostNow, kRtcSyncMinDriftSeconds) == RtcSyncAction::NONE,
           "DS clock 29s behind with the production 30s threshold -> NONE");
    expect(rtcSyncAction(hostNow - kRtcSyncMinDriftSeconds, hostNow, kRtcSyncMinDriftSeconds) == RtcSyncAction::ADVANCE,
           "DS clock 30s behind with the production 30s threshold -> ADVANCE");
}

// Clocks already equal: nothing to do, in either direction.
static void test_equal_clocks_does_not_advance()
{
    const int64_t now = 1000000;
    expect(rtcSyncAction(now, now) == RtcSyncAction::NONE,
           "DS clock equal to host -> NONE");
}

// The direction the task specifically asks to be decided and justified: the DS clock is
// AHEAD of the host (e.g. after running fast-forward for a while). Upstream's own model
// (EmuInstance::syncRTC() unconditionally re-stamping every frame) would step this
// backward; we deliberately never do that, because Pokemon Gen4 titles treat a backward
// RTC jump as tampering and disable Berry growth, the Day Care, Pokerus, etc.
static void test_at_or_ahead_of_host_never_advances()
{
    const int64_t hostNow = 1000000;

    expect(rtcSyncAction(hostNow + 1, hostNow) == RtcSyncAction::NONE,
           "DS clock 1s ahead of host -> NONE (never step backward)");
    expect(rtcSyncAction(hostNow + 3600, hostNow) == RtcSyncAction::NONE,
           "DS clock 1 hour ahead of host (heavy fast-forward) -> NONE (never step backward)");
}

int main()
{
    test_far_behind_host_advances();
    test_just_over_threshold_advances();
    test_under_threshold_does_not_advance();
    test_production_threshold_boundary();
    test_equal_clocks_does_not_advance();
    test_at_or_ahead_of_host_never_advances();

    if (failures > 0)
    {
        std::fprintf(stderr, "%d test(s) failed\n", failures);
        return EXIT_FAILURE;
    }

    std::printf("all tests passed\n");
    return EXIT_SUCCESS;
}
