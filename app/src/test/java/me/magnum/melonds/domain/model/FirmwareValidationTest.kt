// Plain JUnit 4 test (project already depends on junit:junit via `testImplementation(libs.junit)`
// in app/build.gradle.kts -- no new dependency). Runs on the JVM with `./gradlew testDebugUnitTest`,
// no emulator, no Android framework classes involved anywhere in this file or in the production
// code it calls (me.magnum.melonds.domain.model.FirmwareValidation).
//
// Provenance: MELONDS-TESTBED/prototype/FirmwareValidationTest.kt, promoted here as part of
// MELONDS-INTEGRA (see ORDINE.md, step 5/6: this refactor replaces MELONDS-BIOSFIX's inline patch).

package me.magnum.melonds.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FirmwareValidationTest {

    companion object {
        // GBATEK "DS Firmware Header", offset 01Dh ("Console type"), as confirmed on two real
        // firmware dumps in MELONDS-BIOSFIX/ANALISI.md.
        private const val DS_CONSOLE_TYPE = 0xFF
        private const val DSI_CONSOLE_TYPE = 0x57
    }

    @Test
    fun `genuine DS firmware validates in the DS folder at every accepted size`() {
        assertEquals(ConfigurationDirResult.FileStatus.PRESENT, FirmwareValidation.getDsFirmwareStatus(0x20000L, DS_CONSOLE_TYPE))
        assertEquals(ConfigurationDirResult.FileStatus.PRESENT, FirmwareValidation.getDsFirmwareStatus(0x40000L, DS_CONSOLE_TYPE))
        assertEquals(ConfigurationDirResult.FileStatus.PRESENT, FirmwareValidation.getDsFirmwareStatus(0x80000L, DS_CONSOLE_TYPE))
    }

    @Test
    fun `genuine DSi firmware validates in the DSi folder`() {
        assertEquals(ConfigurationDirResult.FileStatus.PRESENT, FirmwareValidation.getDsiFirmwareStatus(0x20000L, DSI_CONSOLE_TYPE))
    }

    @Test
    fun `REGRESSION -- a 128 KiB DSi firmware misfiled into the DS folder is rejected`() {
        // THE reported defect: 0x20000 is a valid DS size too, so size alone cannot tell a
        // DSi firmware placed in the DS folder apart from a genuine DS one.
        // Fails against step1 (PRESENT is returned); passes against step2.
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsFirmwareStatus(0x20000L, DSI_CONSOLE_TYPE))
    }

    @Test
    fun `REGRESSION -- a 128 KiB DS firmware misfiled into the DSi folder is rejected (mirror case)`() {
        // Same mechanism, unreported but closed by the same discriminant (see ANALISI.md):
        // a genuine 128 KiB DS firmware dropped into the DSi folder used to pass on size alone.
        // Fails against step1 (PRESENT is returned); passes against step2.
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsiFirmwareStatus(0x20000L, DS_CONSOLE_TYPE))
    }

    @Test
    fun `an unreadable console-type byte fails open for DS and fails closed for DSi`() {
        // Deliberate asymmetry (see step2-fix.kt doc comments): a read error on the byte must
        // not regress a DS file that was previously accepted on size alone (fail open), but must
        // not let an uninspectable file into the DSi folder either (fail closed).
        assertEquals(ConfigurationDirResult.FileStatus.PRESENT, FirmwareValidation.getDsFirmwareStatus(0x20000L, null))
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsiFirmwareStatus(0x20000L, null))
    }

    @Test
    fun `wrong sizes are always invalid regardless of the console type byte`() {
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsFirmwareStatus(0x12345L, DS_CONSOLE_TYPE))
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsiFirmwareStatus(0x40000L, DSI_CONSOLE_TYPE))
    }
}
