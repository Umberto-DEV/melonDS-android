package me.magnum.melonds.domain.services

import me.magnum.melonds.domain.model.BiosFileClassification
import me.magnum.melonds.domain.model.BiosSlot

/**
 * Works out which [BiosSlot] (if any) a file belongs to purely from its content: its size, and,
 * for firmware-sized files, the console-type byte documented in GBATEK's "DS Firmware Header"
 * (offset 01Dh: 57h identifies a DSi/iQueDSi firmware image, every other observed value a DS
 * family one). The file's current name plays no part in this decision, which is the point: the
 * BIOS sets that circulate use all sorts of names (bios7.bin, biosdsi7.bin, dsifirmware.bin...)
 * and the name never tells which of the two console folders a file actually belongs in.
 *
 * Pure and Android-free on purpose so it can be exercised with plain JUnit.
 */
object BiosFileClassifier {

    /** GBATEK, "DS Firmware Header", offset 01Dh ("Console type"). */
    const val FIRMWARE_CONSOLE_TYPE_OFFSET = 0x1D

    /** GBATEK value for a DSi (or iQueDSi) firmware image at [FIRMWARE_CONSOLE_TYPE_OFFSET]. */
    const val FIRMWARE_CONSOLE_TYPE_DSI = 0x57

    private val firmwareSizes = (BiosSlot.DS_FIRMWARE.expectedSizeBytes.toList() + BiosSlot.DSI_FIRMWARE.expectedSizeBytes.toList()).distinct()

    /**
     * @param sizeBytes the file's exact size.
     * @param firmwareConsoleTypeByte the byte read at [FIRMWARE_CONSOLE_TYPE_OFFSET], or null if
     * it could not be read (e.g. a truncated file) or if [sizeBytes] is not a firmware size, in
     * which case it plays no part in the decision.
     */
    fun classify(sizeBytes: Long, firmwareConsoleTypeByte: Int?): BiosFileClassification {
        return when (sizeBytes) {
            BiosSlot.DS_BIOS7.expectedSizeBytes[0] -> BiosFileClassification.Unambiguous(BiosSlot.DS_BIOS7)
            BiosSlot.DS_BIOS9.expectedSizeBytes[0] -> BiosFileClassification.Unambiguous(BiosSlot.DS_BIOS9)
            // 64 KB: both DSi BIOS images share this size and neither has a documented header
            // field to tell them apart. Content alone cannot resolve this; a caller may still
            // resolve it by other means (e.g. an existing "arm7"/"arm9" hint in the file name).
            BiosSlot.DSI_BIOS7.expectedSizeBytes[0] -> BiosFileClassification.Ambiguous(listOf(BiosSlot.DSI_BIOS7, BiosSlot.DSI_BIOS9))
            in firmwareSizes -> classifyFirmware(sizeBytes, firmwareConsoleTypeByte)
            else -> BiosFileClassification.Unrecognized
        }
    }

    private fun classifyFirmware(sizeBytes: Long, firmwareConsoleTypeByte: Int?): BiosFileClassification {
        val isDsiConsoleType = firmwareConsoleTypeByte == FIRMWARE_CONSOLE_TYPE_DSI
        return when {
            // Only DS accepts these two larger sizes. A DSi console-type byte here would mean
            // the file is internally inconsistent (or not really a firmware dump); don't guess.
            sizeBytes == BiosSlot.DS_FIRMWARE.expectedSizeBytes[1] || sizeBytes == BiosSlot.DS_FIRMWARE.expectedSizeBytes[2] -> {
                if (isDsiConsoleType) BiosFileClassification.Unrecognized else BiosFileClassification.Unambiguous(BiosSlot.DS_FIRMWARE)
            }
            // 128 KB is valid for both consoles; the header byte is the only way to tell them apart.
            sizeBytes == BiosSlot.DSI_FIRMWARE.expectedSizeBytes[0] -> {
                if (isDsiConsoleType) {
                    BiosFileClassification.Unambiguous(BiosSlot.DSI_FIRMWARE)
                } else if (firmwareConsoleTypeByte != null) {
                    BiosFileClassification.Unambiguous(BiosSlot.DS_FIRMWARE)
                } else {
                    // Couldn't read the header byte: don't place a 128 KB file we can't tell apart.
                    BiosFileClassification.Unrecognized
                }
            }
            else -> BiosFileClassification.Unrecognized
        }
    }
}
