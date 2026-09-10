#include "RtcSync.h"

namespace MelonDSAndroid
{

RtcSyncAction rtcSyncAction(int64_t dsEpochSeconds, int64_t hostEpochSeconds, int64_t minDriftSeconds)
{
    int64_t drift = hostEpochSeconds - dsEpochSeconds;

    if (drift >= minDriftSeconds)
    {
        // The DS clock is behind the host: the app was paused/backgrounded (pause()/
        // resume() only touch audio, they never re-sync the RTC -- see MelonDS.cpp), it
        // ran under 100% speed for a while, or it was never set at all. Catching it up is
        // exactly what the option promises, and it is always safe: the RTC-tamper checks
        // used by several DS Pokemon titles only trip on the clock going BACKWARD, never
        // forward.
        return RtcSyncAction::ADVANCE;
    }

    // The DS clock is at, or ahead of, the host. It can end up ahead because fast-forward
    // advances the emulated RTC (which is driven by the scheduler, not the host clock --
    // see RTC::ScheduleTimer) faster than real time passes.
    //
    // melonDS upstream's own "RTC.SyncToHost" option (EmuInstance::syncRTC(), added
    // upstream in 0a65c3785f88 "add RTC sync option (fixes #2589)") does NOT special-case
    // this direction: it unconditionally re-stamps the RTC to host time every single frame
    // while the option is on, so in practice it DOES step the clock backward whenever the
    // emulated clock has pulled ahead. There is no threshold and no anti-regression logic
    // to port from it on this front.
    //
    // We deliberately diverge from upstream here and never step the DS clock backward.
    // Pokemon's Gen4 titles (Diamond/Pearl/Platinum, HeartGold/SoulSilver) detect a
    // backward RTC jump as clock tampering and respond by disabling time-based features
    // (Berry growth, the Day Care, Pokerus, Pal Park, PokeRadar chains...) until the
    // console clock is corrected again through the system settings -- which is the same
    // family of symptom this option exists to fix, so silently rewinding the clock to
    // correct a small fast-forward-induced lead would make the bug WORSE, not better.
    //
    // So: forward corrections only. A clock that has drifted ahead is left alone; nothing
    // here claims that drift is harmless, only that clawing it back automatically is worse
    // than leaving it. It will fall behind again on its own the next time the app is
    // paused or backgrounded, which is the case this option targets.
    return RtcSyncAction::NONE;
}

}
