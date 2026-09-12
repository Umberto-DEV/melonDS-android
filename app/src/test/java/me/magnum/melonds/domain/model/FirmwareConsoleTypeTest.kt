// Plain JUnit 4 test, same pattern as FirmwareValidationTest: pure logic, no Android framework
// classes. Covers the single shared byte-to-state extraction described in
// FirmwareConsoleType's doc comment.
//
// Both directions matter here: a mutant that returns the same FirmwareConsoleType regardless of
// the input byte would pass a suite that only checked one case, so all three distinct outcomes
// (DS, DSI, UNDETERMINED) are asserted below against distinct inputs.

package me.magnum.melonds.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FirmwareConsoleTypeTest {

    @Test
    fun `the DSi header byte reads as DSI`() {
        assertEquals(FirmwareConsoleType.DSI, FirmwareConsoleType.fromHeaderByte(0x57))
    }

    @Test
    fun `a genuine DS header byte reads as DS`() {
        // Real DS sample byte at header offset 0x1D.
        assertEquals(FirmwareConsoleType.DS, FirmwareConsoleType.fromHeaderByte(0xFF))
    }

    @Test
    fun `any non-DSi byte reads as DS, not just the one observed sample`() {
        // GBATEK lists 20h/43h/63h as DS-family values too, in addition to the FFh sample above;
        // the classifier is "not 0x57", not "byte equals a hardcoded DS constant" -- this kills a
        // mutant that only recognized the single sampled DS byte.
        assertEquals(FirmwareConsoleType.DS, FirmwareConsoleType.fromHeaderByte(0x20))
        assertEquals(FirmwareConsoleType.DS, FirmwareConsoleType.fromHeaderByte(0x43))
        assertEquals(FirmwareConsoleType.DS, FirmwareConsoleType.fromHeaderByte(0x63))
    }

    @Test
    fun `a null byte -- unreadable -- reads as UNDETERMINED, not as DS or DSI`() {
        // This is the state the two callers (FirmwareValidation, BiosFileClassifier) disagree
        // about what to DO with -- see FirmwareConsoleTypeDivergenceTest -- but the byte-reading
        // step itself must report it distinctly rather than silently defaulting to DS.
        assertEquals(FirmwareConsoleType.UNDETERMINED, FirmwareConsoleType.fromHeaderByte(null))
    }
}
