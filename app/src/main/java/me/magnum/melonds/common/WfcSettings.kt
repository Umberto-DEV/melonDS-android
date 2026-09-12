package me.magnum.melonds.common

/**
 * Reads and writes the three Wi-fi access point slots of the firmware the console will actually
 * use (see WfcSettingsJNI.cpp):
 *
 *  - built-in firmware in DS mode: the emulator's generated "wfcsettings.bin";
 *  - any other case: the user's own firmware file, DS or DSi, where the slots sit at the offsets
 *    the firmware header declares.
 *
 * The chosen file is the source of truth; nothing is cached in SharedPreferences. The only
 * exception is the free name the user gives to each slot, which the app keeps on its side (see
 * [readLiveSlots] for why the name is not written into the firmware yet).
 */
object WfcSettings {
    /**
     * Reads the three slots from the file at [path]. [isFirmwareFile] tells whether that is a
     * firmware image (DS or DSi, 128/256/512 KB) or the generated "wfcsettings.bin". Each entry
     * is serialized as "enabled;name;primaryDns;secondaryDns" (enabled is "1" or "0", the name is
     * the slot's SSID, the DNS fields are plain IPv4 strings). Returns null if the file doesn't
     * exist, isn't a firmware, or can't be parsed.
     */
    external fun readSlots(path: String, isFirmwareFile: Boolean): Array<String>?

    /**
     * Updates a single slot (0, 1 or 2) in the file at [path], creating the file with three empty
     * slots first if it doesn't exist yet (generated firmware only; a firmware image is never
     * created, only edited in place). [name] is written to the slot's SSID when it isn't empty;
     * an empty name leaves the stored SSID alone, except on a slot that was never configured,
     * which gets the emulated access point's name. [primaryDns] and [secondaryDns] must be valid
     * IPv4 strings. Returns false if the slot index, the name or either address is invalid, or
     * the file couldn't be written.
     */
    external fun writeSlot(
        path: String,
        isFirmwareFile: Boolean,
        slot: Int,
        enabled: Boolean,
        name: String,
        primaryDns: String,
        secondaryDns: String,
    ): Boolean

    /**
     * Reads the three slots out of the firmware the running emulator instance is serving over
     * SPI, in the same format as [readSlots]. Returns null when no session is running.
     *
     * While a session is running the file is NOT the source of truth: the core serves the
     * firmware from its own buffer and its save manager would overwrite the file on the next
     * flush. That is why the settings screen is read-only during a session and the pause menu
     * uses these two entry points instead.
     */
    external fun readLiveSlots(): Array<String>?

    /**
     * Writes one slot into the live firmware buffer (emulator thread stopped for the duration)
     * and requests a flush to the firmware's backing file. The console re-reads the slots from
     * the firmware SPI on every connection attempt, so this applies without a reset.
     *
     * Pass an empty [name] to leave the stored SSID alone. The free name the user chooses is NOT
     * written into the SSID: the emulated access point still answers probe requests with the
     * fixed "melonAP" name (WifiAP.cpp) and, in DSi mode, the Atheros stack compares the
     * requested SSID against that exact string (DSi_NWifi.cpp:1044), so a renamed slot would
     * simply never find the network. Renaming the emulated AP is a separate, later change.
     */
    external fun writeLiveSlot(
        slot: Int,
        enabled: Boolean,
        name: String,
        primaryDns: String,
        secondaryDns: String,
    ): Boolean
}
