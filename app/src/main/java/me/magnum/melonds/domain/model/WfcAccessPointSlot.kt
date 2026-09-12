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
 * One of the three Wi-fi access point slots stored in the emulator's generated "wfcsettings.bin"
 * (see WfcSettingsJNI.cpp). [ssid] is whatever is currently stored in the slot; this screen never
 * changes it, only [enabled], [primaryDns] and [secondaryDns].
 */
data class WfcAccessPointSlot(
    val enabled: Boolean,
    val ssid: String,
    val primaryDns: String,
    val secondaryDns: String,
) {
    fun serialize(): String {
        return "${if (enabled) "1" else "0"};$ssid;$primaryDns;$secondaryDns"
    }

    companion object {
        /**
         * Parses one entry of the array returned by [me.magnum.melonds.common.WfcSettings.readSlots],
         * formatted as "enabled;ssid;primaryDns;secondaryDns". Returns null if [raw] doesn't match
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
                ssid = parts[1],
                primaryDns = parts[2],
                secondaryDns = parts[3],
            )
        }
    }
}
