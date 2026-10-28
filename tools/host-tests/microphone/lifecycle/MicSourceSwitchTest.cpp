// Regression: consuming the blow recording must not leave a device-mic read
// index outside the new device ring when the user changes microphone source.
#include "MelonDSAudio.h"
#include "MicInputOboeCallback.h"
#include <cstdio>
#include <cstring>
#include <vector>
extern std::shared_ptr<oboe::AudioStream> micInputStream;
extern std::shared_ptr<MicInputOboeCallback> micInputCallback;
int runScenario(const char* scenario) {
    if (std::strcmp(scenario, "new-session") == 0) {
        MelonDSAndroid::AudioSettings settings{};
        settings.micSource = 2;
        MelonDSAndroid::setupAudio(settings);
        std::vector<melonDS::s16> firstInput(1024, 10);
        micInputCallback->onAudioReady(micInputStream.get(), firstInput.data(), firstInput.size());
        std::vector<melonDS::s16> previousSession(700);
        if (MelonDSAndroid::readMic(previousSession.data(), previousSession.size()) != 700) return 2;
        MelonDSAndroid::cleanupAudio();
        MelonDSAndroid::setupAudio(settings);
        std::vector<melonDS::s16> nextInput(1024, 200);
        std::fill_n(nextInput.begin(), 128, 100);
        micInputCallback->onAudioReady(micInputStream.get(), nextInput.data(), nextInput.size());
        melonDS::s16 output[8]{};
        const int count = MelonDSAndroid::readMic(output, 8);
        if (count != 8 || output[0] != 1000) {
            std::fprintf(stderr, "FAIL new microphone session: expected 8 samples beginning 1000, got count=%d first=%d (stale read cursor)\n", count, output[0]);
            return 1;
        }
        MelonDSAndroid::cleanupAudio();
        std::puts("PASS new microphone session starts at newly captured data");
        return 0;
    }
    MelonDSAndroid::AudioSettings settings{};
    settings.micSource = 1;
    MelonDSAndroid::setupAudio(settings);
    std::vector<melonDS::s16> blow(1500);
    if (MelonDSAndroid::readMic(blow.data(), blow.size()) != 1500) return 2;
    settings.micSource = 2;
    MelonDSAndroid::updateAudioSettings(settings);
    std::vector<melonDS::s16> input(1024, 100);
    micInputCallback->onAudioReady(micInputStream.get(), input.data(), input.size());
    melonDS::s16 output[8]{};
    const int count = MelonDSAndroid::readMic(output, 8);
    if (count != 8) {
        std::fprintf(stderr,"FAIL expected 8 device samples, got %d\n", count);
        return 1;
    }
    for (auto sample : output) {
        if (sample != 1000) {
            std::fprintf(stderr,"FAIL expected new captured sample 1000, got %d\n", sample);
            return 1;
        }
    }
    MelonDSAndroid::cleanupAudio();
    std::puts("PASS blow to device microphone consumes newly captured samples");
    return 0;
}

int testWrapAndSilence() {
    MelonDSAndroid::AudioSettings settings{};
    settings.micSource = 2;
    MelonDSAndroid::setupAudio(settings);
    melonDS::s16 output[32]{};
    if (MelonDSAndroid::readMic(output, 8) != 0) return 1;
    std::vector<melonDS::s16> input(1024, 100);
    micInputCallback->onAudioReady(micInputStream.get(), input.data(), input.size());
    std::vector<melonDS::s16> consumed(1000);
    if (MelonDSAndroid::readMic(consumed.data(), consumed.size()) != 1000) return 1;
    std::fill(input.begin(), input.end(), 200);
    micInputCallback->onAudioReady(micInputStream.get(), input.data(), input.size());
    if (MelonDSAndroid::readMic(output, 32) != 32) return 1;
    for (int i = 0; i < 32; ++i) {
        const int expected = i < 24 ? 1000 : 2000;
        if (output[i] != expected) {
            std::fprintf(stderr, "FAIL ring order at %d: got %d expected %d\n", i, output[i], expected);
            return 1;
        }
    }
    MelonDSAndroid::userDisableMic();
    if (MelonDSAndroid::readMic(output, 8) != 8) return 1;
    for (int i = 0; i < 8; ++i) if (output[i] != 0) return 1;
    MelonDSAndroid::userEnableMic();
    if (MelonDSAndroid::readMic(output, 8) != 8) return 1;
    for (int i = 0; i < 8; ++i) if (output[i] != 2000) return 1;
    MelonDSAndroid::cleanupAudio();
    std::puts("PASS ring wrap, empty capture, muted silence and resumed queued samples");
    return 0;
}

int main(int argc, char** argv) {
    if (argc == 2) return runScenario(argv[1]);
    if (runScenario("source-switch") || runScenario("new-session")) return 1;
    return testWrapAndSilence();
}
