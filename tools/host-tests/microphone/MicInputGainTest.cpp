// Regression: microphone gain must saturate instead of narrowing out-of-range
// floating-point PCM to s16 (undefined behavior and polarity reversal).
// Literal expected samples are independent of the implementation under test.
#include "MicInputOboeCallback.h"
#include <cstdio>
#include <cstdlib>
#include <vector>
#ifdef __ANDROID__
class TestAudioStream final : public oboe::AudioStream {
public:
    explicit TestAudioStream(int sampleRate) { mSampleRate = sampleRate; }
    oboe::Result requestStart() override { return oboe::Result::OK; }
    oboe::Result requestPause() override { return oboe::Result::OK; }
    oboe::Result requestFlush() override { return oboe::Result::OK; }
    oboe::Result requestStop() override { return oboe::Result::OK; }
    oboe::StreamState getState() override { return oboe::StreamState::Open; }
    oboe::Result waitForStateChange(oboe::StreamState, oboe::StreamState*, int64_t) override {
        return oboe::Result::ErrorUnimplemented;
    }
    bool isXRunCountSupported() const override { return false; }
    oboe::AudioApi getAudioApi() const override { return oboe::AudioApi::Unspecified; }
    void updateFramesWritten() override {}
    void updateFramesRead() override {}
};
#else
using TestAudioStream = oboe::AudioStream;
#endif

struct GainCase { const char* name; melonDS::s16 input; melonDS::s16 expected; };
int main() {
    // A constant signal stays constant through any correct linear resampler.
    const GainCase cases[] = {
        {"silence", 0, 0},
        {"positive quiet", 123, 1230},
        {"negative quiet", -123, -1230},
        {"positive last safe integer", 3276, 32760},
        {"negative last safe integer", -3276, -32760},
        {"positive first clipped integer", 3277, 32767},
        {"negative first clipped integer", -3277, -32768},
        {"positive loud", 4000, 32767},
        {"negative loud", -4000, -32768},
        {"positive full scale", 32767, 32767},
        {"negative full scale", -32768, -32768},
    };
    int failures = 0;
    for (const auto& c : cases) {
        TestAudioStream stream(48000);
        std::mutex mutex;
        MicInputOboeCallback callback(2048, mutex);
        std::vector<melonDS::s16> samples(1024, c.input);
        const auto result = callback.onAudioReady(&stream, samples.data(), samples.size());
        if (result != oboe::DataCallbackResult::Continue || callback.bufferCount == 0) {
            std::fprintf(stderr, "FAIL %s: callback produced no usable samples\n", c.name);
            ++failures;
            continue;
        }
        for (int i = 0; i < callback.bufferCount; ++i) {
            if (callback.buffer[i] != c.expected) {
                std::fprintf(stderr, "FAIL %s: input=%d output[%d]=%d expected=%d\n",
                             c.name, c.input, i, callback.buffer[i], c.expected);
                ++failures;
                break;
            }
        }
    }
    std::printf("%zu gain cases, %d failures\n", sizeof(cases) / sizeof(cases[0]), failures);
    return failures ? EXIT_FAILURE : EXIT_SUCCESS;
}
