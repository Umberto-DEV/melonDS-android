// Plain JUnit 4 test (project already depends on junit:junit via `testImplementation(libs.junit)`
// in app/build.gradle.kts -- no new dependency). Runs on the JVM with `./gradlew testDebugUnitTest`,
// no emulator, no Android framework classes involved anywhere in this file or in the production
// code it calls (me.magnum.melonds.domain.model.FirmwareValidation).

package me.magnum.melonds.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FirmwareValidationTest {

    companion object {
        // GBATEK "DS Firmware Header", offset 01Dh ("Console type"): a real DS sample has 0xFF
        // there, a real DSi sample 0x57.
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
    fun `REGRESSION -- a 128 KiB DSi firmware misfiled into the DS folder is flagged as the wrong console`() {
        // 0x20000 is a valid DS size too, so size alone cannot tell a DSi firmware placed in the
        // DS folder apart from a genuine DS one.
        assertEquals(ConfigurationDirResult.FileStatus.WRONG_CONSOLE, FirmwareValidation.getDsFirmwareStatus(0x20000L, DSI_CONSOLE_TYPE))
    }

    @Test
    fun `REGRESSION -- a 128 KiB DS firmware misfiled into the DSi folder is flagged as the wrong console (mirror case)`() {
        // Same mechanism, mirrored: a genuine 128 KiB DS firmware dropped into the DSi folder
        // would also pass on size alone without the console-type check.
        assertEquals(ConfigurationDirResult.FileStatus.WRONG_CONSOLE, FirmwareValidation.getDsiFirmwareStatus(0x20000L, DS_CONSOLE_TYPE))
    }

    @Test
    fun `an unreadable console-type byte fails open for DS and fails closed for DSi`() {
        // Deliberate asymmetry: a read error on the byte must not regress a DS file that was
        // previously accepted on size alone (fail open), but must not let an uninspectable file
        // into the DSi folder either (fail closed).
        assertEquals(ConfigurationDirResult.FileStatus.PRESENT, FirmwareValidation.getDsFirmwareStatus(0x20000L, null))
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsiFirmwareStatus(0x20000L, null))
    }

    @Test
    fun `wrong sizes are always invalid regardless of the console type byte`() {
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsFirmwareStatus(0x12345L, DS_CONSOLE_TYPE))
        assertEquals(ConfigurationDirResult.FileStatus.INVALID, FirmwareValidation.getDsiFirmwareStatus(0x40000L, DSI_CONSOLE_TYPE))
    }
}
