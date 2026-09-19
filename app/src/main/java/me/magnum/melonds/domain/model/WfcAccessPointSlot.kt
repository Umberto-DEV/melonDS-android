package me.magnum.melonds.domain.model

private val IPV4_COMPONENT_RANGE = 0..255

/**
 * True if [address] is a plain IPv4 address in dotted-decimal form ("a.b.c.d", each part 0-255).
 * No leading/trailing whitespace, no IPv6, no hostnames.
 */
fun isValidIpv4(address: String): Boolean {
    val parts = address.split(".")
    if (parts.size != 4) {
        return false
    }

    return parts.all { part ->
        // Reject empty parts and leading zeroes' ambiguity ("00", "01") by requiring the part to
        // round-trip through Int -> String unchanged; still accept a lone "0".
        val value = part.toIntOrNull() ?: return@all false
        value in IPV4_COMPONENT_RANGE && value.toString() == part
    }
}

/**
 * True if [name] fits in a firmware access point slot's name field: at most
 * [WfcAccessPointSlot.MAX_NAME_LENGTH] bytes, no control characters, and none of the separators
 * used when the slots are handed over the JNI boundary. An empty name is valid and means "leave
 * the stored one alone".
 */
fun isValidWfcSlotName(name: String): Boolean {
    if (name.toByteArray(Charsets.UTF_8).size > WfcAccessPointSlot.MAX_NAME_LENGTH) {
        return false
    }

    return name.none { it == ';' || it.code < 0x20 || it.code == 0x7F }
}

/**
 * One of the three Wi-fi access point slots of the firmware the console will use (see
 * WfcSettingsJNI.cpp). [name] is the name stored in the slot itself (its SSID); the free label
 * the user gives to a connection is kept by the app instead, because the emulated access point
 * still answers with the fixed "melonAP" name.
 */
data class WfcAccessPointSlot(
    val enabled: Boolean,
    val name: String,
    val primaryDns: String,
    val secondaryDns: String,
) {
    fun serialize(): String {
        return "${if (enabled) "1" else "0"};$name;$primaryDns;$secondaryDns"
    }

    companion object {
        /** The firmware's SSID field is 32 bytes, with no terminator when full. */
        const val MAX_NAME_LENGTH = 32

        /**
         * Parses one entry of the array returned by [me.magnum.melonds.common.WfcSettings.readSlots],
         * formatted as "enabled;name;primaryDns;secondaryDns". Returns null if [raw] doesn't match
         * that shape.
         */
        fun parse(raw: String): WfcAccessPointSlot? {
            val parts = raw.split(";", limit = 4)
            if (parts.size != 4) {
                return null
            }

            val enabled = when (parts[0]) {
                "1" -> true
                "0" -> false
                else -> return null
            }

            return WfcAccessPointSlot(
                enabled = enabled,
                name = parts[1],
                primaryDns = parts[2],
                secondaryDns = parts[3],
            )
        }
    }
}
