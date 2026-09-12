// The DS and DSi firmware.bin can be the same size (128 KiB / 0x20000), so file size alone
// cannot tell them apart: a DSi firmware.bin misfiled into the DS folder would still pass DS
// validation on size alone.
//
// Fix: additionally require the "Console type" byte documented in GBATEK's "DS Firmware Header"
// at offset 01Dh to be consistent with the folder being validated (57h = DSi/iQueDSi; anything
// else, or an unreadable byte, is treated as "not DSi").
//
// The console-type byte is read into a [FirmwareConsoleType] by a single shared place instead of
// being compared against 0x57 here directly. This class deliberately fails OPEN on
// FirmwareConsoleType.UNDETERMINED: a byte that could not be read must not be treated as "wrong
// console".

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
