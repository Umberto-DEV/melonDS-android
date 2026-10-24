package me.magnum.melonds.domain.model

/**
 * One of the individual files that a DS or DSi BIOS/firmware directory is expected to contain.
 * The pair (consoleType, canonicalFileName) identifies where a recognised file must end up.
 *
 * NAND storage ([canonicalFileName] "nand.bin") is intentionally not part of this enum: unlike
 * the BIOS and firmware images, it has no documented size or header signature that can be used
 * to recognise it by content, so it can only ever be matched by its existing file name.
 */
enum class BiosSlot(val consoleType: ConsoleType, val canonicalFileName: String, val expectedSizeBytes: LongArray) {
    DS_BIOS7(ConsoleType.DS, "bios7.bin", longArrayOf(0x4000)),
    DS_BIOS9(ConsoleType.DS, "bios9.bin", longArrayOf(0x1000)),
    DS_FIRMWARE(ConsoleType.DS, "firmware.bin", longArrayOf(0x20000, 0x40000, 0x80000)),
    DSI_BIOS7(ConsoleType.DSi, "bios7.bin", longArrayOf(0x10000)),
    DSI_BIOS9(ConsoleType.DSi, "bios9.bin", longArrayOf(0x10000)),
    DSI_FIRMWARE(ConsoleType.DSi, "firmware.bin", longArrayOf(0x20000)),
}

/**
 * Result of inspecting a file's size (and, where relevant, the firmware header's console-type
 * byte) to work out which [BiosSlot] it is meant to fill, without relying on its current name.
 */
sealed class BiosFileClassification {
    /** The file's size (and, for firmware, header byte) match exactly one slot. */
    data class Unambiguous(val slot: BiosSlot) : BiosFileClassification()

    /**
     * The file's size alone cannot tell apart the given slots. This currently only happens for
     * the DSi ARM7 and ARM9 BIOS images: both are exactly 64 KB and have no documented header
     * field that distinguishes them, so content alone cannot pick one.
     */
    data class Ambiguous(val candidateSlots: List<BiosSlot>) : BiosFileClassification()

    /** Neither size nor header byte match any known BIOS/firmware slot. */
    object Unrecognized : BiosFileClassification()
}
