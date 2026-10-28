#pragma once
// Host-only stand-in for the Android hardware boundary. The production sample
// conversion and buffering are compiled unchanged; no signal processing is faked.
#include <cmath>
#include <cstdint>
#include <mutex>
#include <ostream>
namespace oboe {
enum class DataCallbackResult { Continue, Stop };
class AudioStream {
public:
    explicit AudioStream(int32_t sampleRate) : sampleRate_(sampleRate) {}
    int32_t getSampleRate() const { return sampleRate_; }
private:
    int32_t sampleRate_;
};
class AudioStreamCallback {
public:
    virtual ~AudioStreamCallback() = default;
    virtual DataCallbackResult onAudioReady(AudioStream*, void*, int32_t) = 0;
};
}
