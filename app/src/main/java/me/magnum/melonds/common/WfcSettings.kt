package me.magnum.melonds.common

/**
 * Reads and writes the three Wi-fi access point slots stored in the emulator's generated
 * "wfcsettings.bin" file (see WfcSettingsJNI.cpp / EmulatorArgsBuilder.cpp). The file is the
 * source of truth for these settings; nothing is cached in SharedPreferences.
 */
object WfcSettings {
    /**
     * Reads the three slots from the file at [path]. Each entry is serialized as
     * "enabled;ssid;primaryDns;secondaryDns" (enabled is "1" or "0", the DNS fields are plain
     * IPv4 strings). Returns null if the file doesn't exist or can't be parsed.
     */
    external fun readSlots(path: String): Array<String>?

    /**
     * Updates a single slot (0, 1 or 2) in the file at [path], creating the file with three
     * empty slots first if it doesn't exist yet. [primaryDns] and [secondaryDns] must be valid
     * IPv4 strings. Returns false if the slot index or either address is invalid, or the file
     * couldn't be written.
     */
    external fun writeSlot(path: String, slot: Int, enabled: Boolean, primaryDns: String, secondaryDns: String): Boolean
}
