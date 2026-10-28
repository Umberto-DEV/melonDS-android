#pragma once
// Host-only Android stream adapter. Sample production and consumption remain in
// unchanged MicInputOboeCallback.cpp and MelonDSAudio.cpp, not in this fake.
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <memory>
#include <mutex>
#include <ostream>
namespace oboe {
enum class DataCallbackResult { Continue, Stop };
enum class PerformanceMode { LowLatency, None, PowerSaving };
enum class Direction { Input, Output };
enum class AudioFormat { I16 };
enum class SharingMode { Shared, Exclusive };
enum class Usage { Game };
enum class InputPreset { VoiceRecognition };
enum class Result { OK };
enum class StreamState { Open, Closing, Closed };
class AudioStream {
public:
    int32_t getSampleRate() const { return 48000; }
    Result requestStart() { return Result::OK; }
    Result requestStop() { return Result::OK; }
    Result requestPause() { return Result::OK; }
    Result close() { state_ = StreamState::Closed; return Result::OK; }
    StreamState getState() const { return state_; }
    void setPerformanceHintEnabled(bool) {}
    int getBufferCapacityInFrames() const { return 2048; }
    void setBufferSizeInFrames(int) {}
private:
    StreamState state_ = StreamState::Open;
};
class AudioStreamCallback {
public:
    virtual ~AudioStreamCallback() = default;
    virtual DataCallbackResult onAudioReady(AudioStream*, void*, int32_t) = 0;
};
class StabilizedCallback {
public:
    explicit StabilizedCallback(AudioStreamCallback*) {}
};
class AudioStreamBuilder {
public:
    void setChannelCount(int) {}
    void setFramesPerCallback(int) {}
    void setSampleRate(int) {}
    void setFormat(AudioFormat) {}
    void setFormatConversionAllowed(bool) {}
    void setDirection(Direction) {}
    void setPerformanceMode(PerformanceMode) {}
    void setSharingMode(SharingMode) {}
    void setUsage(Usage) {}
    void setInputPreset(InputPreset) {}
    template<typename T> void setDataCallback(std::shared_ptr<T>) {}
    template<typename T> void setErrorCallback(std::shared_ptr<T>) {}
    Result openStream(std::shared_ptr<AudioStream>& out) {
        out = std::make_shared<AudioStream>();
        return Result::OK;
    }
};
}
