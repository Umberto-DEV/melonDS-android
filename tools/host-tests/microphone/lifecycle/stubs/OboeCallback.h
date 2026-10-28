#pragma once
#include <oboe/Oboe.h>
#include "MelonDSAudio.h"
class OboeCallback : public oboe::AudioStreamCallback {
public:
    OboeCallback(int, void(*)()) {}
    void setActiveInstance(std::weak_ptr<MelonDSAndroid::MelonInstance>) {}
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void*, int32_t) override {
        return oboe::DataCallbackResult::Continue;
    }
};
