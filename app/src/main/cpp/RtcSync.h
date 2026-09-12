#ifndef RTCSYNC_H
#define RTCSYNC_H

#include <cstdint>

namespace MelonDSAndroid
{

/**
 * What the "Synchronize clock to device time" option should do this frame, given the DS
 * RTC's current date/time and the host device's current date/time.
 *
 * NONE:    leave the DS clock alone.
 * ADVANCE: the DS clock is behind the host by at least the configured threshold -- stamp
 *          it forward to match the host.
 *
 * There is deliberately no "step the clock backward" action. See rtcSyncAction() in
 * RtcSync.cpp for why.
 */
enum class RtcSyncAction
{
    NONE,
    ADVANCE,
};

/**
 * Production drift threshold for rtcSyncAction(), shared with RtcSyncTest.cpp so the test
 * exercises the same number that ships. The sync is now only ever run on discrete events
 * (emulator resume, first frame after load) rather than every frame, so there is no longer
 * a need to guard against sub-second/rounding differences with a tight 1s threshold --
 * instead this absorbs whole-second rounding and small clock drift between the two clocks
 * without re-stamping the RTC over noise. Real drift this option is meant to catch (time
 * spent paused/backgrounded) is measured in minutes, not seconds, so 30s never masks it.
 */
constexpr int64_t kRtcSyncMinDriftSeconds = 30;

/**
 * Pure decision function behind the RTC-sync option. Takes both clocks as Unix epoch
 * seconds so it can be tested without Android, without the emulator, and without letting
 * real time pass.
 *
 * dsEpochSeconds:   the DS RTC's current date/time.
 * hostEpochSeconds: the host device's current wall-clock date/time.
 * minDriftSeconds:  the DS clock must be behind by at least this many seconds before an
 *                    ADVANCE is requested. Keeps the RTC from being re-stamped over
 *                    sub-second/rounding differences between the two clocks when they are
 *                    otherwise already in sync.
 *
 * Only ever asks to move the DS clock forward, never backward -- even when the DS clock
 * is ahead of the host (e.g. after a fast-forward session).
 */
RtcSyncAction rtcSyncAction(int64_t dsEpochSeconds, int64_t hostEpochSeconds, int64_t minDriftSeconds = kRtcSyncMinDriftSeconds);

}

#endif
