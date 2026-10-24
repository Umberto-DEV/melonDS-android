package me.magnum.melonds.ui.settings.fragments

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            // Explain what is needed before opening the picker, not after it fails. Two folders
            // are unavoidable: DS and DSi use the same three file names for different files, so
            // they cannot share one folder. The one case content cannot resolve is called out
            // explicitly -- the two DSi BIOS images are both exactly 64 KB with nothing in them
            // to tell them apart, so only their names can.
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.bios_guided_setup_title)
                    .setMessage(R.string.bios_guided_setup_explanation)
                    .setPositiveButton(R.string.bios_guided_setup_choose_folder) { _, _ ->
                        guidedSetupLauncher.launch(null)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            true
        }

        consoleTypePreference.setOnPreferenceChangeListener { _, newValue ->
            val consoleTypePreferenceValue = newValue as String
            val newConsoleType = enumValueOfIgnoreCase<ConsoleType>(consoleTypePreferenceValue)

            val validationResult = viewModel.getConsoleConfigurationDirectoryStatus(newConsoleType)
            if (validationResult.status != ConfigurationDirResult.Status.VALID) {
                AlertDialog.Builder(requireContext())
                        .setMessage(getIncorrectBiosDirMessage(validationResult))
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }

            true
        }
    }

    override fun getTitle() = getString(R.string.custom_bios_firmware)

    /**
     * Used to show the same generic "not correct" text for three different problems: a missing
     * file, a file with the wrong size, and a folder that actually holds the other console's
     * BIOS/firmware. Now picked per aggregate state: WRONG_CONSOLE takes priority (it names the
     * likely actual cause via the existing ds/dsi_incorrect_bios_dir_info text), then MISSING
     * files are named explicitly since they're the most actionable problem, then INVALID (wrong
     * size) files.
     */
    private fun getIncorrectBiosDirMessage(validationResult: ConfigurationDirResult): String {
        val wrongConsoleFiles = validationResult.fileResults.filter { it.second == ConfigurationDirResult.FileStatus.WRONG_CONSOLE }.map { it.first }
        val missingFiles = validationResult.fileResults.filter { it.second == ConfigurationDirResult.FileStatus.MISSING }.map { it.first }
        val invalidFiles = validationResult.fileResults.filter { it.second == ConfigurationDirResult.FileStatus.INVALID }.map { it.first }

        return when {
            wrongConsoleFiles.isNotEmpty() -> {
                val textRes = when (validationResult.consoleType) {
                    ConsoleType.DS -> R.string.ds_incorrect_bios_dir_info
                    ConsoleType.DSi -> R.string.dsi_incorrect_bios_dir_info
                }
                getString(textRes)
            }
            missingFiles.isNotEmpty() -> {
                val textRes = when (validationResult.consoleType) {
                    ConsoleType.DS -> R.string.ds_bios_dir_missing_files_info
                    ConsoleType.DSi -> R.string.dsi_bios_dir_missing_files_info
                }
                getString(textRes, missingFiles.joinToString(", "))
            }
            invalidFiles.isNotEmpty() -> {
                val textRes = when (validationResult.consoleType) {
                    ConsoleType.DS -> R.string.ds_bios_dir_invalid_size_files_info
                    ConsoleType.DSi -> R.string.dsi_bios_dir_invalid_size_files_info
                }
                getString(textRes, invalidFiles.joinToString(", "))
            }
            else -> {
                val textRes = when (validationResult.consoleType) {
                    ConsoleType.DS -> R.string.ds_incorrect_bios_dir_info
                    ConsoleType.DSi -> R.string.dsi_incorrect_bios_dir_info
                }
                getString(textRes)
            }
        }
    }

    /**
     * Turns a single folder of loosely-named BIOS/firmware files into the "DS" and "DSi"
     * sub-folders melonDS expects, with the recognised files copied into the right one under
     * the right name, then points the DS/DSi BIOS directory preferences at those sub-folders.
     * See [BiosGuidedFolderSetup] for exactly what is and isn't recognised.
     *
     * The copy itself (which can mean gigabytes, e.g. a DSi nand.bin) runs off the main thread;
     * a non-cancellable progress dialog covers the wait. If the fragment's view is destroyed
     * while it's running, [viewLifecycleOwner]'s scope cancels the job -- [BiosGuidedFolderSetup]
     * cleans up any partially written file when that happens.
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

        val appContext = requireContext().applicationContext
        val progressDialog = AlertDialog.Builder(requireContext())
                .setMessage(R.string.bios_guided_setup_in_progress)
                .setCancelable(false)
                .show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    BiosGuidedFolderSetup(appContext).run(parentDirectoryUri)
                }
                applyGuidedBiosSetupResult(report, parentDirectoryUri, dsBiosDirPreference, dsiBiosDirPreference)
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun applyGuidedBiosSetupResult(
        report: BiosGuidedFolderSetup.Report?,
        parentDirectoryUri: Uri,
        dsBiosDirPreference: BiosDirectoryPickerPreference,
        dsiBiosDirPreference: BiosDirectoryPickerPreference,
    ) {
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

        // Only re-point a preference when the console it belongs to actually ended up complete:
        // an existing, valid directory shouldn't be swapped for one that's still missing files.
        if (allMandatorySlotsSatisfied(report, ConsoleType.DS)) {
            report.dsDirectory?.let { pointPreferenceAt(dsBiosDirPreference, it.uri) }
        }
        if (allMandatorySlotsSatisfied(report, ConsoleType.DSi)) {
            report.dsiDirectory?.let { pointPreferenceAt(dsiBiosDirPreference, it.uri) }
        }

        // pointPreferenceAt() only reliably validates the preference it actually re-pointed
        // (onDirectoryPicked short-circuits on a SecurityException before validating -- see
        // BiosDirectoryPickerPreference); revalidate both explicitly so their displayed status
        // always reflects the outcome of this run.
        dsBiosDirPreference.revalidate()
        dsiBiosDirPreference.revalidate()

        AlertDialog.Builder(requireContext())
                .setTitle(R.string.bios_guided_setup_result_title)
                .setMessage(buildGuidedSetupSummary(report))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    /** True when every slot [BiosGuidedFolderSetup] tracks for [consoleType] was found (whether newly copied or already there). */
    private fun allMandatorySlotsSatisfied(report: BiosGuidedFolderSetup.Report, consoleType: ConsoleType): Boolean {
        val outcomesForConsole = report.slotResults.filter { it.slot.consoleType == consoleType }
        return outcomesForConsole.isNotEmpty() && outcomesForConsole.all {
            it.outcome == BiosGuidedFolderSetup.Outcome.CREATED || it.outcome == BiosGuidedFolderSetup.Outcome.ALREADY_PRESENT
        }
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