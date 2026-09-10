// Defect (confirmed by MELONDS-BIOSFIX, see its ANALISI.md and firmware-validation.candidate.patch):
// getDsFirmwareStatus() accepted 0x20000/0x40000/0x80000 on size alone; getDsiFirmwareStatus()
// accepted only 0x20000, also on size alone. A genuine DSi firmware.bin (128 KiB = 0x20000)
// misfiled into the DS folder therefore passed DS validation, because 0x20000 is a valid size
// for both consoles -- size alone cannot tell them apart.
//
// Fix: additionally require the "Console type" byte documented in GBATEK's "DS Firmware
// Header" at offset 01Dh to be consistent with the folder being validated (57h = DSi/iQueDSi;
// anything else, or an unreadable byte, is treated as "not DSi"). Verified against two real
// firmware dumps in MELONDS-BIOSFIX/ANALISI.md: DS sample has 0xFF at 0x1D, DSi sample has
// 0x57 at 0x1D, matching GBATEK exactly.
//
// This object is the pure, Android-free extraction of the decision logic that used to live
// inline in FileSystemConfigurationDirectoryVerifier (MELONDS-TESTBED refactor); it carries
// the same fix as MELONDS-BIOSFIX/firmware-validation.candidate.patch, verified branch for
// branch to be behaviorally identical -- see MELONDS-INTEGRA/ORDINE.md, step 5/6.

package me.magnum.melonds.domain.model

object FirmwareValidation {
    // GBATEK "DS Firmware Header", offset 01Dh ("Console type"). 57h identifies a DSi (or
    // iQueDSi) firmware image; DS/DS-lite/iQueDS(-lite) images use FFh/20h/43h/63h.
    const val CONSOLE_TYPE_OFFSET = 0x1D
    const val CONSOLE_TYPE_DSI = 0x57

    /**
     * @param size length in bytes of firmware.bin, as reported by the AssetFileDescriptor.
     * @param consoleType the byte at [CONSOLE_TYPE_OFFSET] of the firmware header, or null if
     *   it could not be read (e.g. a truncated stream after the size check already passed).
     */
    fun getDsFirmwareStatus(size: Long, consoleType: Int?): ConfigurationDirResult.FileStatus {
        return when (size) {
            0x20000L,
            0x40000L,
            0x80000L ->
                if (consoleType == CONSOLE_TYPE_DSI) {
                    // Right size for DS, but the header says DSi: this is the confirmed defect.
                    ConfigurationDirResult.FileStatus.INVALID
                } else {
                    // Unknown/unreadable byte fails OPEN here: a read error on a byte outside
                    // the fixed defect must not regress files that were previously PRESENT.
                    ConfigurationDirResult.FileStatus.PRESENT
                }
            else -> ConfigurationDirResult.FileStatus.INVALID
        }
    }

    fun getDsiFirmwareStatus(size: Long, consoleType: Int?): ConfigurationDirResult.FileStatus {
        return when (size) {
            0x20000L ->
                if (consoleType == CONSOLE_TYPE_DSI) {
                    ConfigurationDirResult.FileStatus.PRESENT
                } else {
                    // Right size for DSi, but the header does not say DSi (mirror case: a
                    // genuine DS firmware, or an unreadable byte) fails CLOSED here: we cannot
                    // positively confirm this is a DSi firmware, so we do not call it PRESENT.
                    ConfigurationDirResult.FileStatus.INVALID
                }
            else -> ConfigurationDirResult.FileStatus.INVALID
        }
    }
}
