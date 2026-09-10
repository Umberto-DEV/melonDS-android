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
//
// The console-type byte itself is now read into a [FirmwareConsoleType] by a single shared
// place instead of being compared against 0x57 here directly -- see that type's doc comment
// (MELONDS-INTEGRA/CORREZIONI-PRE-PR.md, correction 7) for why, and for the deliberate
// fail-open policy this class applies to FirmwareConsoleType.UNDETERMINED below.

package me.magnum.melonds.domain.model

object FirmwareValidation {

    /**
     * @param size length in bytes of firmware.bin, as reported by the AssetFileDescriptor.
     * @param consoleType the byte at [FirmwareConsoleType.HEADER_OFFSET] of the firmware header,
     *   or null if it could not be read (e.g. a truncated stream after the size check already
     *   passed).
     */
    fun getDsFirmwareStatus(size: Long, consoleType: Int?): ConfigurationDirResult.FileStatus {
        val type = FirmwareConsoleType.fromHeaderByte(consoleType)
        return when (size) {
            0x20000L,
            0x40000L,
            0x80000L ->
                if (type == FirmwareConsoleType.DSI) {
                    // Right size for DS, but the header says DSi: this is the confirmed defect.
                    ConfigurationDirResult.FileStatus.INVALID
                } else {
                    // DS, or UNDETERMINED (unreadable byte): fail OPEN here so a read error on a
                    // byte outside the fixed defect must not regress files that were previously
                    // PRESENT.
                    ConfigurationDirResult.FileStatus.PRESENT
                }
            else -> ConfigurationDirResult.FileStatus.INVALID
        }
    }

    fun getDsiFirmwareStatus(size: Long, consoleType: Int?): ConfigurationDirResult.FileStatus {
        val type = FirmwareConsoleType.fromHeaderByte(consoleType)
        return when (size) {
            0x20000L ->
                if (type == FirmwareConsoleType.DSI) {
                    ConfigurationDirResult.FileStatus.PRESENT
                } else {
                    // DS, or UNDETERMINED (unreadable byte): fail CLOSED here (mirror case) --
                    // we cannot positively confirm this is a DSi firmware, so we do not call it
                    // PRESENT.
                    ConfigurationDirResult.FileStatus.INVALID
                }
            else -> ConfigurationDirResult.FileStatus.INVALID
        }
    }
}
