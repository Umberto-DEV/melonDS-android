#ifndef AUDIOOUTPUTPOLICY_H
#define AUDIOOUTPUTPOLICY_H

namespace MelonDSAndroid
{

/**
 * Pure decision function behind MelonDSAudio.cpp's updateAudioSettings(): should the audio
 * output stream be active (open) given the *new* settings being applied?
 *
 * Extracted so the condition can be tested without Oboe, without JNI, and without an
 * AudioStream -- and, more importantly, so the call site cannot accidentally go back to
 * comparing against the settings this call is about to replace instead of the settings it
 * was just given. See updateAudioSettings() in MelonDSAudio.cpp for the bug this replaced:
 * it read currentAudioSettings.volume (the value about to be overwritten) instead of the
 * volume argument (the value just requested), so raising the volume from 0 could not make
 * this true on the call that raised it.
 */
bool shouldAudioOutputStreamBeActive(bool soundEnabled, int volume);

}

#endif
