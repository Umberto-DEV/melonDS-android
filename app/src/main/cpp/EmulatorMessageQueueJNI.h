#ifndef MELONDS_ANDROID_MESSAGEQUEUE_JNI_H
#define MELONDS_ANDROID_MESSAGEQUEUE_JNI_H

namespace MelonDSAndroid {
    // Event ids 100-102 and 200-213 belong to AndroidMelonEventMessenger; this one is fired
    // from SaveManager, which is not a MelonEventMessenger, so it lives here instead. Keep in
    // sync with EmulatorEventType.kt.
    static constexpr int EVENT_SAVE_FLUSH_FAILED = 103;

    void fireEmulatorEvent(int type, int dataLength, void* data);
    // inline: the header is included from more than one translation unit.
    inline void fireEmulatorEvent(int type) { fireEmulatorEvent(type, 0, nullptr); };
}

#endif // MELONDS_ANDROID_MESSAGEQUEUE_JNI_H