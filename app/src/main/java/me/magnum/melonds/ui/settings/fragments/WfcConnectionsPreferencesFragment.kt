package me.magnum.melonds.ui.settings.fragments

import android.os.Bundle
import android.widget.Toast
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
import me.magnum.melonds.domain.model.WfcAccessPointSlot
import me.magnum.melonds.domain.model.isValidIpv4
import me.magnum.melonds.ui.settings.PreferenceFragmentTitleProvider
import me.magnum.melonds.ui.settings.SettingsActivity
import java.io.File

/**
 * Lets the user configure the three Wi-fi access point slots used by Nintendo WFC (Mystery Gift,
 * GTS/online battles, and a third free slot). Unlike the rest of the settings screens, these
 * values are NOT stored in SharedPreferences: "wfcsettings.bin" (read/written through
 * [WfcSettings]) is the only source of truth, since that's also what the emulator core reads.
 */
@AndroidEntryPoint
class WfcConnectionsPreferencesFragment : BasePreferenceFragment(), PreferenceFragmentTitleProvider {

    private class SlotWidgets(
        val enabled: SwitchPreferenceCompat,
        val primaryDns: EditTextPreference,
        val secondaryDns: EditTextPreference,
        val recommendedDns: String,
    )

    private lateinit var settingsFilePath: String
    private lateinit var slotWidgets: List<SlotWidgets>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_wfc_connections, rootKey)
        settingsFilePath = File(requireContext().filesDir, WFC_SETTINGS_FILE_NAME).absolutePath

        slotWidgets = listOf(
            buildSlotWidgets(0, RECOMMENDED_MYSTERY_GIFT_DNS),
            buildSlotWidgets(1, RECOMMENDED_GTS_BATTLES_DNS),
            buildSlotWidgets(2, RECOMMENDED_ALTERNATIVE_DNS),
        )

        findPreference<Preference>("wfc_use_recommended_servers")!!.setOnPreferenceClickListener {
            applyRecommendedServers()
            true
        }

        loadCurrentSlots()

        // The core keeps its own SaveManager on this same file while a session is running (even paused);
        // its next flush would silently overwrite anything written here, so lock the screen instead.
        if (requireActivity().intent.getBooleanExtra(SettingsActivity.EXTRA_LAUNCHED_FROM_EMULATOR, false)) {
            findPreference<Preference>("wfc_connections_info")!!.summary = getString(R.string.wfc_close_game_to_change)
            findPreference<Preference>("wfc_use_recommended_servers")!!.isEnabled = false
            slotWidgets.forEach {
                it.enabled.isEnabled = false
                it.primaryDns.isEnabled = false
                it.secondaryDns.isEnabled = false
            }
        }
    }

    private fun buildSlotWidgets(index: Int, recommendedDns: String): SlotWidgets {
        val enabled = findPreference<SwitchPreferenceCompat>("wfc_slot_${index}_enabled")!!
        val primaryDns = findPreference<EditTextPreference>("wfc_slot_${index}_primary_dns")!!
        val secondaryDns = findPreference<EditTextPreference>("wfc_slot_${index}_secondary_dns")!!

        // The file is the source of truth, not SharedPreferences; these widgets only cache what
        // was last read from/written to it.
        enabled.isPersistent = false
        primaryDns.isPersistent = false
        secondaryDns.isPersistent = false

        val widgets = SlotWidgets(enabled, primaryDns, secondaryDns, recommendedDns)

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

    private fun writeSlot(index: Int, enabled: Boolean, primaryDns: String, secondaryDns: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = WfcSettings.writeSlot(settingsFilePath, index, enabled, primaryDns, secondaryDns)
            withContext(Dispatchers.Main) {
                if (!success) {
                    Toast.makeText(requireContext(), R.string.wfc_slot_write_error, Toast.LENGTH_SHORT).show()
                    // The widget already flipped to the new value optimistically; put it back in sync with the file.
                    loadCurrentSlots()
                }
            }
        }
    }

    private fun applyRecommendedServers() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allSucceeded = slotWidgets.withIndex().all { (index, widgets) ->
                WfcSettings.writeSlot(settingsFilePath, index, true, widgets.recommendedDns, widgets.recommendedDns)
            }

            withContext(Dispatchers.Main) {
                loadCurrentSlots()
                Toast.makeText(
                    requireContext(),
                    if (allSucceeded) R.string.wfc_recommended_servers_applied else R.string.wfc_slot_write_error,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun loadCurrentSlots() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rawSlots = WfcSettings.readSlots(settingsFilePath)

            withContext(Dispatchers.Main) {
                if (rawSlots == null) {
                    Toast.makeText(requireContext(), R.string.wfc_read_error, Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                slotWidgets.forEachIndexed { index, widgets ->
                    val slot = rawSlots.getOrNull(index)?.let { WfcAccessPointSlot.parse(it) }
                    widgets.enabled.isChecked = slot?.enabled ?: false
                    widgets.primaryDns.text = slot?.primaryDns ?: DEFAULT_DNS
                    widgets.secondaryDns.text = slot?.secondaryDns ?: DEFAULT_DNS
                }
            }
        }
    }

    override fun getTitle() = getString(R.string.category_wfc_connections)

    private companion object {
        // Must match EmulatorArgsBuilder.cpp's kWifiSettingsPath and the file name used by
        // Platform::OpenInternalFile (relative to the app's internal files dir).
        const val WFC_SETTINGS_FILE_NAME = "wfcsettings.bin"
        const val DEFAULT_DNS = "0.0.0.0"

        // Verified 2026-09-12 with real DNS queries for nas/gpcm/dls1/conntest.nintendowifi.net:
        // WiiLink's resolver answers all four (dls1 = Mystery Gift), Kaeru and AltWFC answer theirs.
        // 167.235.229.36 is NOT WiiLink (it points at Kaeru and has no dls1 record).
        const val RECOMMENDED_MYSTERY_GIFT_DNS = "5.161.56.11" // WiiLink
        const val RECOMMENDED_GTS_BATTLES_DNS = "178.62.43.212" // Kaeru
        const val RECOMMENDED_ALTERNATIVE_DNS = "172.104.88.237" // AltWFC
    }
}
