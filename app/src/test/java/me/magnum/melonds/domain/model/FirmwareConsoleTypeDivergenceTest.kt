// Pins the DELIBERATE divergence between FirmwareValidation (fail-open) and BiosFileClassifier
// (fail-closed) on the exact same undeterminable input, so a future refactor that "aligns" them
// by accident is caught here rather than discovered as a user-visible inconsistency again.
//
// The concrete case that surfaced this (MELONDS-INTEGRA/CORREZIONI-PRE-PR.md, correction 7): a
// 0x20000-byte firmware file whose console-type byte could not be read. Today:
//   - FirmwareValidation.getDsFirmwareStatus  -> PRESENT      (fail open: don't regress a file
//     that size alone used to accept)
//   - BiosFileClassifier.classify             -> Unrecognized (fail closed: don't auto-place a
//     file the guided assistant cannot positively identify)
// A file the guided assistant refuses to place would nonetheless pass validation if the user
// placed it there by hand -- an accepted, declared asymmetry between "validate what exists" and
// "auto-place a new file", not a bug to converge.
//
// Both directions are exercised: each half of this test would fail on its own if a future change
// made the two callers agree (whichever direction), which is exactly the risk this file guards
// against -- see FirmwareConsoleType's doc comment for why the two policies are intentionally
// different.

package me.magnum.melonds.domain.model

import me.magnum.melonds.domain.services.BiosFileClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class FirmwareConsoleTypeDivergenceTest {

    private companion object {
        // 0x20000 (128 KiB) is the one size shared by DS_FIRMWARE and DSI_FIRMWARE (see BiosSlot),
        // which is exactly why the console-type byte is the only thing that can tell them apart --
        // and exactly why an unreadable byte at this size is the case where the two callers'
        // policies are visible.
        const val AMBIGUOUS_FIRMWARE_SIZE = 0x20000L
        val UNREADABLE_CONSOLE_TYPE_BYTE: Int? = null
    }

    @Test
    fun `an unreadable header byte makes FirmwareValidation accept a DS-folder firmware (fail open)`() {
        assertEquals(
            ConfigurationDirResult.FileStatus.PRESENT,
            FirmwareValidation.getDsFirmwareStatus(AMBIGUOUS_FIRMWARE_SIZE, UNREADABLE_CONSOLE_TYPE_BYTE),
        )
    }

    @Test
    fun `the very same unreadable header byte makes BiosFileClassifier refuse to auto-place the file (fail closed)`() {
        assertEquals(
            BiosFileClassification.Unrecognized,
            BiosFileClassifier.classify(AMBIGUOUS_FIRMWARE_SIZE, UNREADABLE_CONSOLE_TYPE_BYTE),
        )
    }
}
