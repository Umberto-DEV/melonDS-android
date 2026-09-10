package me.magnum.melonds.domain.services

import me.magnum.melonds.domain.model.BiosFileClassification
import me.magnum.melonds.domain.model.BiosSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiosFileClassifierTest {

    @Test
    fun `16KB file is unambiguously the DS ARM7 BIOS`() {
        val result = BiosFileClassifier.classify(0x4000, null)
        assertEquals(BiosFileClassification.Unambiguous(BiosSlot.DS_BIOS7), result)
    }

    @Test
    fun `4KB file is unambiguously the DS ARM9 BIOS`() {
        val result = BiosFileClassifier.classify(0x1000, null)
        assertEquals(BiosFileClassification.Unambiguous(BiosSlot.DS_BIOS9), result)
    }

    @Test
    fun `64KB file cannot be told apart between DSi ARM7 and ARM9 by content`() {
        val result = BiosFileClassifier.classify(0x10000, null)
        assertTrue(result is BiosFileClassification.Ambiguous)
        val ambiguous = result as BiosFileClassification.Ambiguous
        assertEquals(listOf(BiosSlot.DSI_BIOS7, BiosSlot.DSI_BIOS9), ambiguous.candidateSlots)
    }

    @Test
    fun `256KB and 512KB firmware are unambiguously DS regardless of header byte read failure`() {
        assertEquals(BiosFileClassification.Unambiguous(BiosSlot.DS_FIRMWARE), BiosFileClassifier.classify(0x40000, null))
        assertEquals(BiosFileClassification.Unambiguous(BiosSlot.DS_FIRMWARE), BiosFileClassifier.classify(0x80000, null))
    }

    @Test
    fun `256KB firmware with a DSi header byte is rejected instead of guessed`() {
        val result = BiosFileClassifier.classify(0x40000, BiosFileClassifier.FIRMWARE_CONSOLE_TYPE_DSI)
        assertEquals(BiosFileClassification.Unrecognized, result)
    }

    @Test
    fun `128KB firmware with DS header byte is DS firmware, this is the misfiled-DSi-firmware defect`() {
        // A real DS sample's byte at 0x1D is 0xFF (see MELONDS-BIOSFIX/ANALISI.md).
        val result = BiosFileClassifier.classify(0x20000, 0xFF)
        assertEquals(BiosFileClassification.Unambiguous(BiosSlot.DS_FIRMWARE), result)
    }

    @Test
    fun `128KB firmware with DSi header byte is DSi firmware`() {
        // A real DSi sample's byte at 0x1D is 0x57 (see MELONDS-BIOSFIX/ANALISI.md).
        val result = BiosFileClassifier.classify(0x20000, 0x57)
        assertEquals(BiosFileClassification.Unambiguous(BiosSlot.DSI_FIRMWARE), result)
    }

    @Test
    fun `128KB firmware with unreadable header byte is not placed anywhere`() {
        val result = BiosFileClassifier.classify(0x20000, null)
        assertEquals(BiosFileClassification.Unrecognized, result)
    }

    @Test
    fun `sizes that match nothing are unrecognized`() {
        assertEquals(BiosFileClassification.Unrecognized, BiosFileClassifier.classify(123, null))
        assertEquals(BiosFileClassification.Unrecognized, BiosFileClassifier.classify(0, null))
    }
}
