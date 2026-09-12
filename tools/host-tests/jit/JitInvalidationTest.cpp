/*
 * Exercises the JIT's block invalidation with a synthetic cartridge: a handful of ARM
 * instructions assembled by this file, wrapped in an NDS header, direct-booted into main RAM.
 * No commercial ROM and no BIOS dump are involved, so this test always runs.
 *
 * Three properties are checked, each of which a specific past bug broke:
 *
 *  1. a literal held in main RAM is folded into the compiled block, and rewriting it drops
 *     the block so the next execution sees the new value;
 *  2. rewriting one 16-byte chunk drops only the blocks that actually cover that chunk, not
 *     every block sharing the surrounding 512-byte range;
 *  3. a literal the JIT cannot localise to a code region (here: one living in the DTCM) is
 *     left alone -- neither registered for invalidation nor folded into the code -- so the
 *     value read stays correct even though nothing is watching that address.
 *
 * The number of live blocks is read straight out of ARMJIT::JitBlocks9. That is a public
 * member today but it is marked "TODO: Encapsulate" upstream; if it ever becomes private,
 * property 2 needs another way to count blocks (the correctness of the emulation alone does
 * not reveal over-invalidation -- discarding a still-valid block is always safe, just slow).
 */

#include <cstdio>
#include <cstring>
#include <memory>
#include <vector>

#include <ARMJIT.h>
#include <Args.h>
#include <MemConstants.h>
#include <NDS.h>
#include <NDSCart.h>
#include <NDS_Header.h>

#include "JitTestSupport.h"

using namespace melonDS;

// Where direct boot drops our ARM9 binary. Everything below lives inside the first 1 KiB, so
// the code and its literals share one 512-byte AddressRange -- which is the point of case 2.
static constexpr u32 CodeBase = 0x02000000;
static constexpr u32 Arm7Base = 0x037F8000;
static constexpr u32 Arm9BinarySize = 0x400;

// The ARM9 binary is padded out to Arm9BinarySize with NOPs; direct boot copies it verbatim.
static constexpr u32 Nop = 0xE1A00000;

/// LDR Rd, [PC, #imm]. R15 reads as the instruction's address plus 8.
static u32 LdrPcRelative(u32 rd, u32 at, u32 target) { return 0xE59F0000 | (rd << 12) | (target - (at + 8)); }
/// STR Rd, [PC, #imm].
static u32 StrPcRelative(u32 rd, u32 at, u32 target) { return 0xE58F0000 | (rd << 12) | (target - (at + 8)); }
/// B <target>.
static u32 BranchTo(u32 at, u32 target) { return 0xEA000000 | (((static_cast<s32>(target) - static_cast<s32>(at + 8)) >> 2) & 0xFFFFFF); }
/// BX Rm. An indirect branch, which ends a JIT block instead of being followed into it.
static u32 BranchExchange(u32 rm) { return 0xE12FFF10 | rm; }

class SyntheticRom
{
public:
    SyntheticRom() : Words(Arm9BinarySize / 4, Nop) {}

    void Put(u32 address, u32 word) { Words[(address - CodeBase) / 4] = word; }

    std::vector<u8> Build(u32 entryPoint) const
    {
        const u32 arm9Size = static_cast<u32>(Words.size()) * 4;
        const u32 arm7Word = 0xEAFFFFFE; // B . -- the ARM7 has nothing to do here.

        std::vector<u8> rom(0x8000 + arm9Size + 4, 0);

        NDSHeader header {};
        memcpy(header.GameTitle, "MELONJIT", 8);
        memcpy(header.GameCode, "MJIT", 4);
        // 0x8000 keeps the ROM out of both the encrypted-secure-area path (which applies
        // between 0x4000 and 0x8000) and the homebrew path (below 0x4000, which would pull in
        // DLDI patching and an SD card).
        header.ARM9ROMOffset = 0x8000;
        header.ARM9EntryAddress = entryPoint;
        header.ARM9RAMAddress = CodeBase;
        header.ARM9Size = arm9Size;
        header.ARM7ROMOffset = 0x8000 + arm9Size;
        header.ARM7EntryAddress = Arm7Base;
        header.ARM7RAMAddress = Arm7Base;
        header.ARM7Size = 4;

        memcpy(rom.data(), &header, sizeof(header));
        memcpy(rom.data() + 0x8000, Words.data(), arm9Size);
        memcpy(rom.data() + 0x8000 + arm9Size, &arm7Word, 4);
        return rom;
    }

private:
    std::vector<u32> Words;
};

static std::unique_ptr<NDS> Boot(const std::vector<u8>& rom, u32 entryPoint)
{
    NDSArgs args {};
    args.JIT = JITArgs();

    auto nds = std::make_unique<NDS>(std::move(args), nullptr);
    nds->SetNDSCart(NDSCart::ParseROM(rom.data(), static_cast<u32>(rom.size()), nullptr));
    nds->Reset();
    nds->SetupDirectBoot("melonds-jit-test.nds");
    nds->Start();
    (void)entryPoint;
    return nds;
}

static void RunFrames(NDS& nds, int count)
{
    for (int i = 0; i < count; i++)
        nds.RunFrame();
}

// -- 1: a literal in main RAM is invalidated when it is rewritten ---------------------------

static void test_rewriting_a_literal_recompiles_the_block()
{
    printf("== a literal in main RAM is dropped when it is rewritten ==\n");

    constexpr u32 Literal = CodeBase + 0x100;
    constexpr u32 Result = CodeBase + 0x300;

    SyntheticRom rom;
    rom.Put(CodeBase + 0, LdrPcRelative(0, CodeBase + 0, Literal));
    rom.Put(CodeBase + 4, StrPcRelative(0, CodeBase + 4, Result));
    rom.Put(CodeBase + 8, BranchTo(CodeBase + 8, CodeBase));
    rom.Put(Literal, 0x11111111);
    rom.Put(Result, 0);

    auto nds = Boot(rom.Build(CodeBase), CodeBase);
    RunFrames(*nds, 4);

    const u32 before = nds->ARM9Read32(Result);
    printf("  after boot:  result=%08x, %zu block(s) compiled\n", before, nds->JIT.JitBlocks9.size());
    JitTest::Expect(before == 0x11111111, "the loop published the original literal");
    JitTest::Expect(nds->JIT.JitBlocks9.size() == 1, "exactly one ARM9 block was compiled");

    nds->ARM9Write32(Literal, 0x22222222);
    printf("  after write: %zu block(s) left\n", nds->JIT.JitBlocks9.size());
    JitTest::Expect(nds->JIT.JitBlocks9.empty(), "rewriting the literal dropped the block");

    RunFrames(*nds, 4);
    const u32 after = nds->ARM9Read32(Result);
    printf("  after rerun: result=%08x, %zu block(s) compiled\n", after, nds->JIT.JitBlocks9.size());
    JitTest::Expect(after == 0x22222222, "the recompiled block published the new literal");
}

// -- 2: invalidation is precise to 16 bytes, not to the whole 512-byte range -----------------

static void test_only_the_written_chunk_is_invalidated()
{
    printf("== rewriting one 16-byte chunk spares the other blocks in the same range ==\n");

    // Both blocks sit in the 512-byte range at 0x02000000: block A in chunk 0, block B in
    // chunk 4. They jump to each other indirectly (BX), because a plain B would let the JIT
    // follow the branch and merge the two into a single block.
    constexpr u32 BlockA = CodeBase + 0x000;
    constexpr u32 BlockB = CodeBase + 0x040;
    constexpr u32 LiteralA = CodeBase + 0x100;
    constexpr u32 LiteralB = CodeBase + 0x104;
    constexpr u32 PointerToA = CodeBase + 0x108;
    constexpr u32 PointerToB = CodeBase + 0x10C;
    constexpr u32 ResultA = CodeBase + 0x300;
    constexpr u32 ResultB = CodeBase + 0x304;

    SyntheticRom rom;
    rom.Put(BlockA + 0, LdrPcRelative(0, BlockA + 0, LiteralA));
    rom.Put(BlockA + 4, StrPcRelative(0, BlockA + 4, ResultA));
    rom.Put(BlockA + 8, LdrPcRelative(2, BlockA + 8, PointerToB));
    rom.Put(BlockA + 12, BranchExchange(2));

    rom.Put(BlockB + 0, LdrPcRelative(1, BlockB + 0, LiteralB));
    rom.Put(BlockB + 4, StrPcRelative(1, BlockB + 4, ResultB));
    rom.Put(BlockB + 8, LdrPcRelative(3, BlockB + 8, PointerToA));
    rom.Put(BlockB + 12, BranchExchange(3));

    rom.Put(LiteralA, 0x11111111);
    rom.Put(LiteralB, 0x22222222);
    rom.Put(PointerToA, BlockA);
    rom.Put(PointerToB, BlockB);

    auto nds = Boot(rom.Build(BlockA), BlockA);
    RunFrames(*nds, 4);

    const size_t compiled = nds->JIT.JitBlocks9.size();
    printf("  after boot:  resultA=%08x resultB=%08x, %zu block(s) compiled\n",
           nds->ARM9Read32(ResultA), nds->ARM9Read32(ResultB), compiled);
    JitTest::Expect(compiled == 2, "both blocks were compiled separately");

    // Rewrite the first instruction of block A so that it now loads block B's literal. Only
    // chunk 0 is touched, so only block A should go.
    nds->ARM9Write32(BlockA + 0, LdrPcRelative(0, BlockA + 0, LiteralB));
    const size_t remaining = nds->JIT.JitBlocks9.size();
    printf("  after write: %zu block(s) left (precise invalidation leaves 1; "
           "over-invalidation leaves 0)\n", remaining);
    JitTest::Expect(remaining == 1, "only the block covering the written chunk was dropped");

    RunFrames(*nds, 4);
    printf("  after rerun: resultA=%08x resultB=%08x, %zu block(s) compiled\n",
           nds->ARM9Read32(ResultA), nds->ARM9Read32(ResultB), nds->JIT.JitBlocks9.size());
    JitTest::Expect(nds->ARM9Read32(ResultA) == 0x22222222, "the rewritten block A ran its new code");
}

// -- 3: a literal outside any code region is neither registered nor folded -------------------

static void test_a_literal_in_dtcm_is_not_folded()
{
    printf("== a literal in the DTCM is left untracked and unfolded ==\n");

    // ARMJIT::LocaliseCodeAddress returns 0 for the DTCM, because CodeMemRegions has no entry
    // for it. Two things must then happen, and this case covers both:
    //   * CompileBlock must not register the literal. Without the `translatedAddr &&` guard it
    //     records an address range of 0 and later indexes CodeMemRegions[0], which is NULL --
    //     a straight null dereference, so dropping the guard turns this test into a crash.
    //   * the backend must not fold the value, which is what FetchedInstr::LiteralRegistered
    //     tells it. Folding an address nothing is watching yields a stale read forever, so
    //     dropping that check makes this test report the old value below.
    // The DTCM is moved next to the code because a PC-relative load only reaches +/-4095
    // bytes; its default home at 0x03000000 is far out of range.
    constexpr u32 DtcmBase = 0x02001000;
    constexpr u32 DtcmSizeSetting = 3 << 1;          // 0x200 << 3 == 4 KiB, the smallest allowed
    constexpr u32 Literal = DtcmBase;                 // 0xFF8 bytes past the load's R15: in range
    constexpr u32 Result = CodeBase + 0x300;

    SyntheticRom rom;
    rom.Put(CodeBase + 0, LdrPcRelative(0, CodeBase + 0, Literal));
    rom.Put(CodeBase + 4, StrPcRelative(0, CodeBase + 4, Result));
    rom.Put(CodeBase + 8, BranchTo(CodeBase + 8, CodeBase));
    rom.Put(Result, 0);

    auto nds = Boot(rom.Build(CodeBase), CodeBase);

    nds->ARM9.CP15Write(0x910, DtcmBase | DtcmSizeSetting);
    const bool mapped = nds->ARM9.DTCMBase == DtcmBase;
    printf("  DTCM mapped at %08x (mask %08x)\n", nds->ARM9.DTCMBase, nds->ARM9.DTCMMask);
    JitTest::Expect(mapped, "the DTCM was remapped next to the code");

    auto writeDtcm = [&nds](u32 address, u32 value) {
        memcpy(&nds->ARM9.DTCM[address & (DTCMPhysicalSize - 1)], &value, sizeof(value));
    };

    writeDtcm(Literal, 0xAAAA1111);
    RunFrames(*nds, 4);
    const u32 before = nds->ARM9Read32(Result);
    printf("  after boot:  result=%08x, %zu block(s) compiled\n", before, nds->JIT.JitBlocks9.size());
    JitTest::Expect(before == 0xAAAA1111, "the loop published the DTCM value");

    // Nothing invalidates the block: the DTCM is not a watched code region. The new value can
    // only surface if the load was compiled as a real memory access.
    writeDtcm(Literal, 0xBBBB2222);
    RunFrames(*nds, 4);
    const u32 after = nds->ARM9Read32(Result);
    printf("  after DTCM rewrite: result=%08x, %zu block(s) compiled\n",
           after, nds->JIT.JitBlocks9.size());
    JitTest::Expect(after == 0xBBBB2222, "the untracked literal was re-read instead of folded");
}

int main()
{
    // stdout is block-buffered when redirected, the core logs to stderr: without this the
    // core's warnings land in the middle of an unrelated section of the report.
    setvbuf(stdout, nullptr, _IOLBF, 0);

    test_rewriting_a_literal_recompiles_the_block();
    test_only_the_written_chunk_is_invalidated();
    test_a_literal_in_dtcm_is_not_folded();

    printf("%s\n", JitTest::Failures == 0 ? "PASS" : "FAIL");
    return JitTest::Failures == 0 ? 0 : 1;
}
