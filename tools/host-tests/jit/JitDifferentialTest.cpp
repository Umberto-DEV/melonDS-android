/*
 * Runs the same ROM twice -- once with the JIT recompiler, once with the interpreter -- and
 * checks that the two produce the same picture.
 *
 * The only sound oracle here is the framebuffer. The JIT and the interpreter do not agree on
 * where the ARM7 happens to be at the end of a given frame, and main RAM holds timing-derived
 * state, so those are printed as diagnostics and never asserted on: making them assertions
 * would produce a test that fails for reasons that are not bugs. Both screens are hashed
 * every 60 frames rather than only at the end, so a divergence is caught while it is still
 * small enough to bisect.
 *
 * Needs a ROM, which cannot live in the repository: point MELONDS_TEST_ROM at one. Without it
 * the test skips (and passes). No BIOS is needed -- the core boots on FreeBIOS via direct
 * boot, which is what NDSArgs defaults to.
 */

#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <memory>
#include <optional>
#include <vector>

#include <Args.h>
#include <GPU.h>
#include <NDS.h>
#include <NDSCart.h>

#include "JitTestSupport.h"

using namespace melonDS;

static constexpr int TotalFrames = 600;
static constexpr int HashInterval = 60;

struct Sample
{
    int Frame;
    uint64_t TopScreen;
    uint64_t BottomScreen;
};

struct RunResult
{
    std::vector<Sample> Samples;
    double Milliseconds;
    uint64_t MainRAM;
    u32 PC9;
    u32 PC7;
};

static std::vector<u8> ReadWholeFile(const char* path)
{
    FILE* file = fopen(path, "rb");
    if (!file)
        return {};

    fseek(file, 0, SEEK_END);
    const long length = ftell(file);
    fseek(file, 0, SEEK_SET);

    std::vector<u8> data(static_cast<size_t>(length));
    if (fread(data.data(), 1, data.size(), file) != data.size())
        data.clear();

    fclose(file);
    return data;
}

static uint64_t HashScreen(NDS& nds, int screen)
{
    const u32* framebuffer = nds.GPU.Framebuffer[nds.GPU.FrontBuffer][screen].get();
    return framebuffer ? JitTest::Hash(framebuffer, 256 * 192 * sizeof(u32)) : 0;
}

static RunResult Run(const std::vector<u8>& rom, bool jit)
{
    NDSArgs args {};
    args.JIT = jit ? std::optional<JITArgs>(JITArgs()) : std::nullopt;

    auto nds = std::make_unique<NDS>(std::move(args), nullptr);
    nds->SetNDSCart(NDSCart::ParseROM(rom.data(), static_cast<u32>(rom.size()), nullptr));
    nds->Reset();
    // The overload taking a name is the one that jumps to the cartridge's entry point; the
    // argument-less NDS::SetupDirectBoot() only lays out memory.
    nds->SetupDirectBoot("melonds-jit-test.nds");
    nds->Start();

    RunResult result {};
    const auto start = std::chrono::steady_clock::now();
    for (int frame = 1; frame <= TotalFrames; frame++)
    {
        nds->RunFrame();
        if (frame % HashInterval == 0)
            result.Samples.push_back({ frame, HashScreen(*nds, 0), HashScreen(*nds, 1) });
    }
    const auto end = std::chrono::steady_clock::now();

    result.Milliseconds = std::chrono::duration<double, std::milli>(end - start).count();
    result.MainRAM = JitTest::Hash(nds->MainRAM, 0x400000);
    result.PC9 = nds->ARM9.R[15];
    result.PC7 = nds->ARM7.R[15];
    return result;
}

int main()
{
    // stdout is block-buffered when redirected, the core logs to stderr: without this the
    // core's warnings land in the middle of an unrelated section of the report.
    setvbuf(stdout, nullptr, _IOLBF, 0);

    const char* romPath = getenv("MELONDS_TEST_ROM");
    if (!romPath || !*romPath)
    {
        printf("SKIP: MELONDS_TEST_ROM is not set (point it at an .nds ROM to run this test)\n");
        return 0;
    }

    const std::vector<u8> rom = ReadWholeFile(romPath);
    if (rom.empty())
    {
        printf("SKIP: could not read the ROM at %s\n", romPath);
        return 0;
    }

    printf("ROM: %s (%zu bytes), %d frames per instance\n", romPath, rom.size(), TotalFrames);

    const RunResult withJit = Run(rom, true);
    const RunResult withInterpreter = Run(rom, false);

    printf("  JIT         %8.1f ms (%.2f ms/frame)  mainram=%016llx pc9=%08x pc7=%08x\n",
           withJit.Milliseconds, withJit.Milliseconds / TotalFrames,
           static_cast<unsigned long long>(withJit.MainRAM), withJit.PC9, withJit.PC7);
    printf("  interpreter %8.1f ms (%.2f ms/frame)  mainram=%016llx pc9=%08x pc7=%08x\n",
           withInterpreter.Milliseconds, withInterpreter.Milliseconds / TotalFrames,
           static_cast<unsigned long long>(withInterpreter.MainRAM), withInterpreter.PC9,
           withInterpreter.PC7);
    printf("  (main RAM and the ARM7 PC are expected to differ: both hold timing-derived state)\n");

    if (withJit.Samples.size() != withInterpreter.Samples.size())
    {
        JitTest::Expect(false, "both runs produced the same number of samples");
        return 1;
    }

    for (size_t i = 0; i < withJit.Samples.size(); i++)
    {
        const Sample& a = withJit.Samples[i];
        const Sample& b = withInterpreter.Samples[i];
        const bool same = a.TopScreen == b.TopScreen && a.BottomScreen == b.BottomScreen;

        char description[160];
        snprintf(description, sizeof(description),
                 "frame %3d: jit %016llx/%016llx == interpreter %016llx/%016llx",
                 a.Frame,
                 static_cast<unsigned long long>(a.TopScreen),
                 static_cast<unsigned long long>(a.BottomScreen),
                 static_cast<unsigned long long>(b.TopScreen),
                 static_cast<unsigned long long>(b.BottomScreen));
        JitTest::Expect(same, description);
    }

    printf("%s\n", JitTest::Failures == 0 ? "PASS" : "FAIL");
    return JitTest::Failures == 0 ? 0 : 1;
}
