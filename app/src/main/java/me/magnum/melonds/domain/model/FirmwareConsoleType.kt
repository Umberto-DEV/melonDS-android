package me.magnum.melonds.domain.model

/**
 * The three possible readings of the console-type byte in a DS/DSi firmware header (GBATEK,
 * "DS Firmware Header", offset 01Dh): [DSI] when the byte is 57h (also used by iQueDSi), [DS] for
 * every other observed value (FFh/20h/43h/63h and others, DS/DS-lite/iQueDS(-lite)), and
 * [UNDETERMINED] when the byte itself could not be read (e.g. a truncated stream).
 *
 * Single shared place to turn that byte into a decision, used by both [FirmwareValidation] and
 * [me.magnum.melonds.domain.services.BiosFileClassifier] so they cannot disagree on it.
 *
 * What still differs between the two callers -- deliberately -- is what they each DO with
 * [UNDETERMINED]: [FirmwareValidation] fails OPEN (a read error never regresses a file that size
 * alone used to accept), while [me.magnum.melonds.domain.services.BiosFileClassifier] fails
 * CLOSED (refuses to auto-place a file it cannot positively identify). A read error is not the
 * same thing as a wrong console, so the two operations weigh it differently on purpose.
 */
enum class FirmwareConsoleType {
    DS,
    DSI,
    UNDETERMINED;

    companion object {
        /** GBATEK "DS Firmware Header", offset 01Dh ("Console type"). */
        const val HEADER_OFFSET = 0x1D

        /** GBATEK value identifying a DSi (or iQueDSi) firmware image at [HEADER_OFFSET]. */
        const val DSI_HEADER_BYTE = 0x57

        /**
         * @param consoleTypeByte the byte at [HEADER_OFFSET], or null if it could not be read.
         */
        fun fromHeaderByte(consoleTypeByte: Int?): FirmwareConsoleType {
            return when (consoleTypeByte) {
                null -> UNDETERMINED
                DSI_HEADER_BYTE -> DSI
                else -> DS
            }
        }
    }
}
