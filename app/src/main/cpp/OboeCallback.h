#ifndef MELONDS_OBOECALLBACK_H
#define MELONDS_OBOECALLBACK_H

#include <oboe/Oboe.h>
#include <fstream>
#include <mutex>
#include "MelonInstance.h"

class OboeCallback : public oboe::AudioStreamCallback {
private:
    int _volume;
    void (*onErrorCallback)(void);
    std::ostream* _recordingStream;
    float audioSampleFrac;

    // Written from the JNI thread while the real-time callback may be reading it, so it is
    // never touched without the mutex. The callback only ever try_locks it (see onAudioReady).
    std::mutex instanceMutex;
    std::weak_ptr<MelonDSAndroid::MelonInstance> activeInstance;

public:
    OboeCallback(int volume, void (*onErrorCallback)(void)) : OboeCallback(volume, onErrorCallback, nullptr) { };
    OboeCallback(int volume, void (*onErrorCallback)(void), std::ostream* recordingStream);
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result result) override;
    void setActiveInstance(std::weak_ptr<MelonDSAndroid::MelonInstance> instance);

private:
    int getNumSamplesOut(int len);
};


#endif //MELONDS_OBOECALLBACK_H
