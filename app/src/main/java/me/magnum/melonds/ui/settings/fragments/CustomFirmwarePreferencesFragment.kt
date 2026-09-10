package me.magnum.melonds.ui.settings.fragments

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.R
import me.magnum.melonds.common.BiosGuidedFolderSetup
import me.magnum.melonds.common.DirectoryAccessValidator
import me.magnum.melonds.common.Permission
import me.magnum.melonds.common.UriPermissionManager
import me.magnum.melonds.common.contracts.DirectoryPickerContract
import me.magnum.melonds.domain.model.ConfigurationDirResult
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.ui.settings.viewmodel.CustomFirmwareViewModel
import me.magnum.melonds.ui.settings.PreferenceFragmentHelper
import me.magnum.melonds.ui.settings.PreferenceFragmentTitleProvider
import me.magnum.melonds.ui.settings.preferences.BiosDirectoryPickerPreference
import me.magnum.melonds.utils.enumValueOfIgnoreCase
import javax.inject.Inject

@AndroidEntryPoint
class CustomFirmwarePreferencesFragment : BasePreferenceFragment(), PreferenceFragmentTitleProvider {

    private val viewModel by viewModels<CustomFirmwareViewModel>()
    private val helper by lazy { PreferenceFragmentHelper(this, uriPermissionManager, directoryAccessValidator) }
    @Inject lateinit var uriPermissionManager: UriPermissionManager
    @Inject lateinit var directoryAccessValidator: DirectoryAccessValidator

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_custom_firmware, rootKey)
        val consoleTypePreference = findPreference<ListPreference>("console_type")!!
        val dsBiosDirPreference = findPreference<BiosDirectoryPickerPreference>("bios_dir")!!
        val dsiBiosDirPreference = findPreference<BiosDirectoryPickerPreference>("dsi_bios_dir")!!

        helper.setupStoragePickerPreference(dsBiosDirPreference)
        helper.setupStoragePickerPreference(dsiBiosDirPreference)

        val biosValidator = object : BiosDirectoryPickerPreference.BiosDirectoryValidator {
            override fun getBiosDirectoryValidationResult(consoleType: ConsoleType, directory: Uri?): ConfigurationDirResult {
                return viewModel.getConsoleConfigurationDirectoryStatus(consoleType, directory)
            }
        }

        dsBiosDirPreference.setBiosDirectoryValidator(biosValidator)
        dsiBiosDirPreference.setBiosDirectoryValidator(biosValidator)

        val guidedSetupLauncher = registerForActivityResult(DirectoryPickerContract(Permission.READ_WRITE)) { uri ->
            if (uri != null) {
                runGuidedBiosSetup(uri, dsBiosDirPreference, dsiBiosDirPreference)
            }
        }
        findPreference<Preference>("guided_bios_setup")?.setOnPreferenceClickListener {
            guidedSetupLauncher.launch(null)
            true
        }

        consoleTypePreference.setOnPreferenceChangeListener { _, newValue ->
            val consoleTypePreferenceValue = newValue as String
            val newConsoleType = enumValueOfIgnoreCase<ConsoleType>(consoleTypePreferenceValue)

            val validationResult = viewModel.getConsoleConfigurationDirectoryStatus(newConsoleType)
            if (validationResult.status != ConfigurationDirResult.Status.VALID) {
                val textRes = when (validationResult.consoleType) {
                    ConsoleType.DS -> R.string.ds_incorrect_bios_dir_info
                    ConsoleType.DSi -> R.string.dsi_incorrect_bios_dir_info
                }

                AlertDialog.Builder(requireContext())
                        .setMessage(textRes)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }

            true
        }
    }

    override fun getTitle() = getString(R.string.custom_bios_firmware)

    /**
     * Turns a single folder of loosely-named BIOS/firmware files into the "DS" and "DSi"
     * sub-folders melonDS expects, with the recognised files copied into the right one under
     * the right name, then points the DS/DSi BIOS directory preferences at those sub-folders.
     * See [BiosGuidedFolderSetup] for exactly what is and isn't recognised.
     */
    private fun runGuidedBiosSetup(parentDirectoryUri: Uri, dsBiosDirPreference: BiosDirectoryPickerPreference, dsiBiosDirPreference: BiosDirectoryPickerPreference) {
        if (directoryAccessValidator.getDirectoryAccessForPermission(parentDirectoryUri, Permission.READ_WRITE) != DirectoryAccessValidator.DirectoryAccessResult.OK) {
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.error_invalid_directory)
                    .setMessage(R.string.error_invalid_directory_description)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            return
        }

        val report = BiosGuidedFolderSetup(requireContext()).run(parentDirectoryUri)
        if (report == null) {
            AlertDialog.Builder(requireContext())
                    .setMessage(R.string.bios_guided_setup_failed)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            return
        }

        // One permission grant on the picked folder covers the DS/DSi sub-folders created or
        // found inside it; each is then persisted as its own console's BIOS directory.
        uriPermissionManager.persistDirectoryPermissions(parentDirectoryUri, Permission.READ_WRITE)
        report.dsDirectory?.takeIf { it.listFiles().isNotEmpty() }?.let { pointPreferenceAt(dsBiosDirPreference, it.uri) }
        report.dsiDirectory?.takeIf { it.listFiles().isNotEmpty() }?.let { pointPreferenceAt(dsiBiosDirPreference, it.uri) }

        AlertDialog.Builder(requireContext())
                .setTitle(R.string.bios_guided_setup_result_title)
                .setMessage(buildGuidedSetupSummary(report))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    /**
     * Points a BIOS directory preference at a sub-folder created inside an already-persisted
     * parent tree. The preference also tries to persist a permission of its own for this exact
     * URI, which is redundant (the parent grant already covers it, see [runGuidedBiosSetup]) but
     * harmless if the platform rejects a persistable request for a URI that was never itself the
     * direct subject of a document-picker grant; either way access to the sub-folder is retained.
     */
    private fun pointPreferenceAt(preference: BiosDirectoryPickerPreference, directoryUri: Uri) {
        try {
            preference.onDirectoryPicked(directoryUri)
        } catch (e: SecurityException) {
            // Ignored: see the doc comment above.
        }
    }

    private fun buildGuidedSetupSummary(report: BiosGuidedFolderSetup.Report): String {
        val lines = report.slotResults.map { result ->
            val consoleLabel = getString(if (result.slot.consoleType == ConsoleType.DS) R.string.console_ds else R.string.console_dsi)
            when (result.outcome) {
                BiosGuidedFolderSetup.Outcome.CREATED -> {
                    val source = result.sourceFileNames.firstOrNull().orEmpty()
                    if (result.resolvedByFileNameHint) {
                        getString(R.string.bios_guided_setup_created_by_name_hint, consoleLabel, result.slot.canonicalFileName, source)
                    } else {
                        getString(R.string.bios_guided_setup_created, consoleLabel, result.slot.canonicalFileName, source)
                    }
                }
                BiosGuidedFolderSetup.Outcome.ALREADY_PRESENT -> getString(R.string.bios_guided_setup_already_present, consoleLabel, result.slot.canonicalFileName)
                BiosGuidedFolderSetup.Outcome.MISSING -> getString(R.string.bios_guided_setup_missing, consoleLabel, result.slot.canonicalFileName)
                BiosGuidedFolderSetup.Outcome.AMBIGUOUS -> getString(R.string.bios_guided_setup_ambiguous, consoleLabel, result.slot.canonicalFileName, result.sourceFileNames.joinToString(", "))
            }
        }.toMutableList()

        val dsiLabel = getString(R.string.console_dsi)
        lines += when (report.nandResult.outcome) {
            BiosGuidedFolderSetup.Outcome.CREATED -> getString(R.string.bios_guided_setup_created, dsiLabel, BiosGuidedFolderSetup.NAND_FILE_NAME, report.nandResult.sourceFileName.orEmpty())
            BiosGuidedFolderSetup.Outcome.ALREADY_PRESENT -> getString(R.string.bios_guided_setup_already_present, dsiLabel, BiosGuidedFolderSetup.NAND_FILE_NAME)
            else -> getString(R.string.bios_guided_setup_missing, dsiLabel, BiosGuidedFolderSetup.NAND_FILE_NAME)
        }

        return lines.joinToString("\n") { "• $it" }
    }
}