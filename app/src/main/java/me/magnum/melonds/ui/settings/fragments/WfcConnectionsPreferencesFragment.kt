package me.magnum.melonds.ui.settings.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.magnum.melonds.R
import me.magnum.melonds.common.WfcSettings
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.WfcAccessPointSlot
import me.magnum.melonds.domain.model.isValidIpv4
import me.magnum.melonds.domain.model.isValidWfcSlotName
import me.magnum.melonds.domain.repositories.SettingsRepository
import me.magnum.melonds.ui.settings.PreferenceFragmentTitleProvider
import me.magnum.melonds.ui.settings.SettingsActivity
import java.io.File
import javax.inject.Inject

/**
 * Lets the user configure the three Wi-fi access point slots used by Nintendo WFC (Mystery Gift,
 * GTS/online battles, and a third free slot). Unlike the rest of the settings screens, the
 * enabled flag and the DNS servers are NOT stored in SharedPreferences: the firmware the console
 * will boot with is the only source of truth, since that's also what the emulator core reads.
 *
 * Which firmware that is depends on the same two settings the emulator looks at when it builds
 * its firmware save manager (MelonInstance's constructor): the generated firmware in DS mode
 * keeps its Wi-fi settings in "wfcsettings.bin", every other case (custom BIOS, or DSi) uses the
 * user's own firmware file. See [WfcTarget].
 *
 * The free name of each connection is the exception: it lives in SharedPreferences
 * ("wfc_slot_name_1..3") and is not written into the slot's SSID, because the emulated access
 * point answers probe requests with the fixed "melonAP" name (WifiAP.cpp) and the DSi wifi stack
 * compares the requested SSID against that exact string (DSi_NWifi.cpp:1044) -- a renamed slot
 * would never find the network. Teaching the emulated AP to answer with the slot's name is a
 * separate change.
 */
@AndroidEntryPoint
class WfcConnectionsPreferencesFragment : BasePreferenceFragment(), PreferenceFragmentTitleProvider {

    @Inject lateinit var settingsRepository: SettingsRepository

    /** The firmware whose Wi-fi slots this screen reads and writes. */
    private sealed interface WfcTarget {
        /** Path or document URI, as [WfcSettings] expects it. */
        val path: String
        val isFirmwareFile: Boolean
        val storageSummary: Int

        /** The emulator's own "wfcsettings.bin", used by the generated firmware in DS mode. */
        data class Internal(override val path: String) : WfcTarget {
            override val isFirmwareFile = false
            override val storageSummary = R.string.wfc_storage_internal
        }

        /** The user's firmware file. [directory] and [fileName] are only needed for the backup. */
        data class FirmwareFile(
            override val path: String,
            val isDsi: Boolean,
            val directory: Uri,
            val fileName: String,
        ) : WfcTarget {
            override val isFirmwareFile = true
            override val storageSummary = if (isDsi) R.string.wfc_storage_dsi_firmware else R.string.wfc_storage_ds_firmware
        }
    }

    private class SlotWidgets(
        val name: EditTextPreference,
        val enabled: SwitchPreferenceCompat,
        val primaryDns: EditTextPreference,
        val secondaryDns: EditTextPreference,
        val recommendedDns: String,
    )

    private lateinit var appContext: Context
    private lateinit var slotWidgets: List<SlotWidgets>
    private var target: WfcTarget? = null
    private var isReadOnly = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_wfc_connections, rootKey)
        appContext = requireContext().applicationContext

        slotWidgets = listOf(
            buildSlotWidgets(0, RECOMMENDED_MYSTERY_GIFT_DNS),
            buildSlotWidgets(1, RECOMMENDED_GTS_BATTLES_DNS),
            buildSlotWidgets(2, RECOMMENDED_ALTERNATIVE_DNS),
        )

        findPreference<Preference>("wfc_use_recommended_servers")!!.setOnPreferenceClickListener {
            applyRecommendedServers()
            true
        }

        // The core keeps the firmware in its own buffer while a session is running (even paused)
        // and its save manager would silently overwrite anything written here, so lock the screen
        // instead. Turning connections on and off mid-game goes through the pause menu, which
        // edits the live firmware.
        isReadOnly = requireActivity().intent.getBooleanExtra(SettingsActivity.EXTRA_LAUNCHED_FROM_EMULATOR, false)
        if (isReadOnly) {
            findPreference<Preference>("wfc_connections_info")!!.summary = getString(R.string.wfc_close_game_to_change)
            findPreference<Preference>("wfc_use_recommended_servers")!!.isEnabled = false
            slotWidgets.forEach {
                it.enabled.isEnabled = false
                it.primaryDns.isEnabled = false
                it.secondaryDns.isEnabled = false
            }
        }

        lifecycleScope.launch {
            target = resolveTarget()
            findPreference<Preference>("wfc_storage_location")!!.summary = target?.let { getString(it.storageSummary) }
                ?: getString(R.string.wfc_firmware_not_found)

            if (target == null) {
                // Nothing to read or write: a custom BIOS directory without a firmware file.
                setSlotsEnabled(false)
            } else {
                loadCurrentSlots()
            }
        }
    }

    private fun buildSlotWidgets(index: Int, recommendedDns: String): SlotWidgets {
        // The name is app-side only, so unlike the other widgets it keeps its SharedPreferences
        // value. Its key is 1-based, matching the slot numbers the user sees.
        val name = findPreference<EditTextPreference>("wfc_slot_name_${index + 1}")!!
        val enabled = findPreference<SwitchPreferenceCompat>("wfc_slot_${index}_enabled")!!
        val primaryDns = findPreference<EditTextPreference>("wfc_slot_${index}_primary_dns")!!
        val secondaryDns = findPreference<EditTextPreference>("wfc_slot_${index}_secondary_dns")!!

        // The firmware is the source of truth for these three, not SharedPreferences; the widgets
        // only cache what was last read from/written to it.
        enabled.isPersistent = false
        primaryDns.isPersistent = false
        secondaryDns.isPersistent = false

        if (name.text.isNullOrEmpty()) {
            name.text = getString(R.string.wfc_slot_default_name, index + 1)
        }
        name.setOnPreferenceChangeListener { _, newValue ->
            if (isValidWfcSlotName(newValue as String)) {
                true
            } else {
                Toast.makeText(requireContext(), R.string.wfc_invalid_name, Toast.LENGTH_SHORT).show()
                false
            }
        }

        val widgets = SlotWidgets(name, enabled, primaryDns, secondaryDns, recommendedDns)

        enabled.setOnPreferenceChangeListener { _, newValue ->
            writeSlot(index, newValue as Boolean, widgets.primaryDns.text ?: DEFAULT_DNS, widgets.secondaryDns.text ?: DEFAULT_DNS)
            true
        }
        primaryDns.setOnPreferenceChangeListener { _, newValue ->
            onDnsChanged(index, widgets, primaryDns = newValue as String, secondaryDns = null)
        }
        secondaryDns.setOnPreferenceChangeListener { _, newValue ->
            onDnsChanged(index, widgets, primaryDns = null, secondaryDns = newValue as String)
        }

        return widgets
    }

    private fun onDnsChanged(index: Int, widgets: SlotWidgets, primaryDns: String?, secondaryDns: String?): Boolean {
        val newPrimaryDns = primaryDns ?: widgets.primaryDns.text ?: DEFAULT_DNS
        val newSecondaryDns = secondaryDns ?: widgets.secondaryDns.text ?: DEFAULT_DNS

        if (!isValidIpv4(newPrimaryDns) || !isValidIpv4(newSecondaryDns)) {
            Toast.makeText(requireContext(), R.string.wfc_invalid_ip_address, Toast.LENGTH_SHORT).show()
            return false
        }

        writeSlot(index, widgets.enabled.isChecked, newPrimaryDns, newSecondaryDns)
        return true
    }

    /**
     * Picks the firmware the console will use, with the same conditions as the emulator's own
     * firmware save manager: built-in firmware in DS mode keeps its Wi-fi settings in
     * "wfcsettings.bin", every other case boots from the user's firmware file. Returns null when
     * a firmware file is expected but missing.
     */
    private suspend fun resolveTarget(): WfcTarget? = withContext(Dispatchers.IO) {
        val consoleType = settingsRepository.getDefaultConsoleType()
        val isDsi = consoleType == ConsoleType.DSi

        if (!settingsRepository.useCustomBios() && !isDsi) {
            return@withContext WfcTarget.Internal(File(appContext.filesDir, WFC_SETTINGS_FILE_NAME).absolutePath)
        }

        val directoryUri = (if (isDsi) settingsRepository.getDsiBiosDirectory() else settingsRepository.getDsBiosDirectory())
            ?: return@withContext null
        val directory = DocumentFile.fromTreeUri(appContext, directoryUri) ?: return@withContext null
        val firmware = directory.findFile(FIRMWARE_FILE_NAME) ?: return@withContext null

        WfcTarget.FirmwareFile(
            path = firmware.uri.toString(),
            isDsi = isDsi,
            directory = directoryUri,
            fileName = firmware.name ?: FIRMWARE_FILE_NAME,
        )
    }

    /**
     * Copies the user's firmware file next to itself as "<name>.bak" before the very first write,
     * once per console type. A corrupted firmware would cost the user their console identity
     * (nickname, MAC, WFC ID), which is not something a settings screen should be able to do
     * without a way back.
     */
    private fun ensureFirmwareBackup(target: WfcTarget.FirmwareFile): Boolean {
        val preferences = preferenceManager.sharedPreferences ?: return false
        val key = if (target.isDsi) BACKUP_DONE_DSI_KEY else BACKUP_DONE_DS_KEY
        if (preferences.getBoolean(key, false)) {
            return true
        }

        return try {
            val directory = DocumentFile.fromTreeUri(appContext, target.directory) ?: return false
            val backupName = "${target.fileName}.$BACKUP_EXTENSION"
            val existing = directory.findFile(backupName)
            if (existing != null && existing.length() > 0) {
                // A backup from an earlier install is still a backup: don't overwrite it with a
                // firmware this screen may already have edited.
                preferences.edit { putBoolean(key, true) }
                return true
            }

            val backup = existing ?: directory.createFile(BACKUP_MIME_TYPE, backupName) ?: return false
            appContext.contentResolver.openInputStream(target.path.toUri())?.use { input ->
                appContext.contentResolver.openOutputStream(backup.uri, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: return false
            } ?: return false

            preferences.edit { putBoolean(key, true) }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to back up the firmware file", e)
            false
        }
    }

    private fun writeSlot(index: Int, enabled: Boolean, primaryDns: String, secondaryDns: String) {
        val target = target ?: return

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { writeSlotBlocking(target, index, enabled, primaryDns, secondaryDns) }
            if (result != WriteResult.OK) {
                Toast.makeText(requireContext(), result.messageResource, Toast.LENGTH_SHORT).show()
                // The widget already flipped to the new value optimistically; put it back in sync
                // with the firmware.
                loadCurrentSlots()
            }
        }
    }

    private fun writeSlotBlocking(target: WfcTarget, index: Int, enabled: Boolean, primaryDns: String, secondaryDns: String): WriteResult {
        if (target is WfcTarget.FirmwareFile && !ensureFirmwareBackup(target)) {
            return WriteResult.BACKUP_FAILED
        }

        // The name stays out of the firmware on purpose: see the class comment. An empty name
        // tells the native side to leave the slot's SSID alone.
        val written = WfcSettings.writeSlot(target.path, target.isFirmwareFile, index, enabled, "", primaryDns, secondaryDns)
        return if (written) WriteResult.OK else WriteResult.WRITE_FAILED
    }

    private fun applyRecommendedServers() {
        val target = target ?: return

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                slotWidgets.withIndex()
                    .map { (index, widgets) -> writeSlotBlocking(target, index, true, widgets.recommendedDns, widgets.recommendedDns) }
                    .firstOrNull { it != WriteResult.OK }
                    ?: WriteResult.OK
            }

            loadCurrentSlots()
            Toast.makeText(
                requireContext(),
                if (result == WriteResult.OK) R.string.wfc_recommended_servers_applied else result.messageResource,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun loadCurrentSlots() {
        val target = target ?: return

        lifecycleScope.launch {
            val rawSlots = withContext(Dispatchers.IO) {
                // With a session running the file is behind the core's own buffer, so show what
                // the console is actually serving. Outside a session there is nothing live to
                // read and readLiveSlots() returns null.
                (if (isReadOnly) WfcSettings.readLiveSlots() else null)
                    ?: WfcSettings.readSlots(target.path, target.isFirmwareFile)
            }
            if (rawSlots == null) {
                Toast.makeText(requireContext(), R.string.wfc_read_error, Toast.LENGTH_SHORT).show()
                return@launch
            }

            slotWidgets.forEachIndexed { index, widgets ->
                val slot = rawSlots.getOrNull(index)?.let { WfcAccessPointSlot.parse(it) }
                widgets.enabled.isChecked = slot?.enabled ?: false
                widgets.primaryDns.text = slot?.primaryDns ?: DEFAULT_DNS
                widgets.secondaryDns.text = slot?.secondaryDns ?: DEFAULT_DNS
            }
        }
    }

    private fun setSlotsEnabled(enabled: Boolean) {
        findPreference<Preference>("wfc_use_recommended_servers")!!.isEnabled = enabled
        slotWidgets.forEach {
            it.enabled.isEnabled = enabled
            it.primaryDns.isEnabled = enabled
            it.secondaryDns.isEnabled = enabled
        }
    }

    private enum class WriteResult(val messageResource: Int) {
        OK(0),
        BACKUP_FAILED(R.string.wfc_firmware_backup_error),
        WRITE_FAILED(R.string.wfc_slot_write_error),
    }

    override fun getTitle() = getString(R.string.category_wfc_connections)

    private companion object {
        const val TAG = "WfcConnections"

        // Must match EmulatorArgsBuilder.cpp's kWifiSettingsPath and the file name used by
        // Platform::OpenInternalFile (relative to the app's internal files dir).
        const val WFC_SETTINGS_FILE_NAME = "wfcsettings.bin"

        // Same name SharedPreferencesSettingsRepository looks for in the BIOS directories.
        const val FIRMWARE_FILE_NAME = "firmware.bin"
        const val BACKUP_EXTENSION = "bak"
        const val BACKUP_MIME_TYPE = "application/octet-stream"
        const val BACKUP_DONE_DS_KEY = "wfc_ds_firmware_backed_up"
        const val BACKUP_DONE_DSI_KEY = "wfc_dsi_firmware_backed_up"

        const val DEFAULT_DNS = "0.0.0.0"

        // Verified 2026-09-12 with real DNS queries for nas/gpcm/dls1/conntest.nintendowifi.net:
        // WiiLink's resolver answers all four (dls1 = Mystery Gift), Kaeru and AltWFC answer theirs.
        // 167.235.229.36 is NOT WiiLink (it points at Kaeru and has no dls1 record).
        const val RECOMMENDED_MYSTERY_GIFT_DNS = "5.161.56.11" // WiiLink
        const val RECOMMENDED_GTS_BATTLES_DNS = "178.62.43.212" // Kaeru
        const val RECOMMENDED_ALTERNATIVE_DNS = "172.104.88.237" // AltWFC
    }
}
