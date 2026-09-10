package me.magnum.melonds.common

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import me.magnum.melonds.domain.model.BiosFileClassification
import me.magnum.melonds.domain.model.BiosSlot
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.FirmwareConsoleType
import me.magnum.melonds.domain.services.BiosFileClassifier
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Turns a single folder full of loosely-named BIOS/firmware files (as they are commonly shared:
 * mixed together, named after whatever the source used) into the two folders melonDS actually
 * expects, with the files it recognises copied into the right one under the right name.
 *
 * What it does, given one folder the user already granted access to:
 * 1. Creates "DS" and "DSi" sub-folders in it if they are not already there.
 * 2. Looks at every loose file directly inside it (not inside DS/DSi themselves) and classifies
 *    it with [BiosFileClassifier], using size and, for firmware, the header byte at offset 0x1D.
 * 3. Copies every file it can place unambiguously into the matching sub-folder under its
 *    canonical name (bios7.bin, bios9.bin, firmware.bin). The DSi ARM7/ARM9 BIOS pair (both
 *    64 KB, no content signature) is resolved by elimination when only one of the two is still
 *    missing, and otherwise by a same-console filename hint ("7"/"arm7" vs "9"/"arm9") -- both
 *    documented per-outcome, since neither is a content-based decision.
 * 4. A file named exactly "nand.bin" (case-insensitively) is copied across as-is: NAND storage
 *    has no documented size or header signature, so it can only ever be recognised by name.
 *
 * Nothing already present at a destination is ever overwritten or deleted, and no source file is
 * ever deleted or moved: only copies are made, so a mistake here never loses the user's files.
 */
class BiosGuidedFolderSetup(private val context: Context) {

    enum class Outcome {
        /** A file was found and copied into place under its canonical name. */
        CREATED,
        /** The destination already had this file; it was left untouched. */
        ALREADY_PRESENT,
        /** No file matching this slot was found among the loose files. */
        MISSING,
        /** More than one candidate file could fill this slot and none could be ruled out. */
        AMBIGUOUS,
    }

    data class SlotResult(
        val slot: BiosSlot,
        val outcome: Outcome,
        /** Name(s) of the loose source file(s) involved, for [Outcome.CREATED] and [Outcome.AMBIGUOUS]. */
        val sourceFileNames: List<String> = emptyList(),
        /** True when a DSi ARM7/ARM9 assignment was made from the file name, not from content. */
        val resolvedByFileNameHint: Boolean = false,
    )

    data class NandResult(val outcome: Outcome, val sourceFileName: String? = null)

    data class Report(
        val dsDirectory: DocumentFile?,
        val dsiDirectory: DocumentFile?,
        val slotResults: List<SlotResult>,
        val nandResult: NandResult,
    )

    /**
     * @param parentDirectoryUri a tree URI the app already has read/write access to (e.g. just
     * returned by the system directory picker). Must point at an existing directory.
     * @return null if [parentDirectoryUri] does not resolve to a directory.
     */
    fun run(parentDirectoryUri: Uri): Report? {
        val parentDir = DocumentFile.fromTreeUri(context, parentDirectoryUri)?.takeIf { it.isDirectory } ?: return null

        val dsDir = parentDir.findFile(DS_FOLDER_NAME)?.takeIf { it.isDirectory } ?: parentDir.createDirectory(DS_FOLDER_NAME)
        val dsiDir = parentDir.findFile(DSI_FOLDER_NAME)?.takeIf { it.isDirectory } ?: parentDir.createDirectory(DSI_FOLDER_NAME)

        val looseFiles = parentDir.listFiles().filter { it.isFile }

        val unambiguousMatches = mutableMapOf<BiosSlot, MutableList<DocumentFile>>()
        val dsiBios7Or9Candidates = mutableListOf<DocumentFile>()
        var nandCandidate: DocumentFile? = null

        for (file in looseFiles) {
            if (file.name?.equals(NAND_FILE_NAME, ignoreCase = true) == true) {
                nandCandidate = file
                continue
            }

            val size = getFileSize(file) ?: continue
            val discriminant = if (size in FIRMWARE_SIZES) readFirmwareConsoleTypeByte(file) else null

            when (val classification = BiosFileClassifier.classify(size, discriminant)) {
                is BiosFileClassification.Unambiguous -> unambiguousMatches.getOrPut(classification.slot) { mutableListOf() }.add(file)
                is BiosFileClassification.Ambiguous -> dsiBios7Or9Candidates.add(file)
                BiosFileClassification.Unrecognized -> Unit
            }
        }

        val slotResults = mutableListOf<SlotResult>()
        for (slot in listOf(BiosSlot.DS_BIOS7, BiosSlot.DS_BIOS9, BiosSlot.DS_FIRMWARE, BiosSlot.DSI_FIRMWARE)) {
            val destDir = if (slot.consoleType == ConsoleType.DS) dsDir else dsiDir
            slotResults += resolveSingleCandidateSlot(destDir, slot, unambiguousMatches[slot].orEmpty())
        }
        slotResults += resolveDsiBiosPair(dsiDir, dsiBios7Or9Candidates)

        val nandResult = resolveNand(dsiDir, nandCandidate)

        return Report(dsDir, dsiDir, slotResults, nandResult)
    }

    private fun resolveSingleCandidateSlot(destDir: DocumentFile?, slot: BiosSlot, candidates: List<DocumentFile>): SlotResult {
        if (destDir == null) {
            return SlotResult(slot, Outcome.MISSING)
        }
        if (destDir.findFile(slot.canonicalFileName) != null) {
            return SlotResult(slot, Outcome.ALREADY_PRESENT)
        }
        return when (candidates.size) {
            0 -> SlotResult(slot, Outcome.MISSING)
            1 -> {
                val source = candidates[0]
                if (copyInto(source, destDir, slot.canonicalFileName)) {
                    SlotResult(slot, Outcome.CREATED, listOf(source.name.orEmpty()))
                } else {
                    SlotResult(slot, Outcome.MISSING)
                }
            }
            else -> SlotResult(slot, Outcome.AMBIGUOUS, candidates.mapNotNull { it.name })
        }
    }

    private fun resolveDsiBiosPair(dsiDir: DocumentFile?, candidates: List<DocumentFile>): List<SlotResult> {
        val bios7 = BiosSlot.DSI_BIOS7
        val bios9 = BiosSlot.DSI_BIOS9

        if (dsiDir == null) {
            return listOf(SlotResult(bios7, Outcome.MISSING), SlotResult(bios9, Outcome.MISSING))
        }

        val bios7Present = dsiDir.findFile(bios7.canonicalFileName) != null
        val bios9Present = dsiDir.findFile(bios9.canonicalFileName) != null
        val missingSlots = listOfNotNull(bios7.takeUnless { bios7Present }, bios9.takeUnless { bios9Present })

        if (missingSlots.isEmpty()) {
            return listOf(SlotResult(bios7, Outcome.ALREADY_PRESENT), SlotResult(bios9, Outcome.ALREADY_PRESENT))
        }
        if (candidates.isEmpty()) {
            return missingSlots.map { SlotResult(it, Outcome.MISSING) } + presentSlotResults(bios7Present, bios9Present)
        }

        // Exactly one 64 KB candidate and exactly one missing slot: no other file could fill it.
        if (missingSlots.size == 1 && candidates.size == 1) {
            val slot = missingSlots[0]
            val source = candidates[0]
            val created = copyInto(source, dsiDir, slot.canonicalFileName)
            val results = mutableListOf<SlotResult>()
            results += if (created) SlotResult(slot, Outcome.CREATED, listOf(source.name.orEmpty())) else SlotResult(slot, Outcome.MISSING)
            results += presentSlotResults(bios7Present, bios9Present)
            return results
        }

        // Try a same-console filename hint ("...7..."/"arm7" vs "...9.../arm9") for whatever
        // remains unresolved. This is explicitly a name-based fallback, not a content decision.
        val remainingCandidates = candidates.toMutableList()
        val results = mutableListOf<SlotResult>()
        for (slot in missingSlots) {
            val hintDigit = if (slot == bios7) "7" else "9"
            val hinted = remainingCandidates.firstOrNull { file ->
                val name = file.name?.lowercase().orEmpty()
                name.contains("arm$hintDigit") || Regex("(^|[^0-9])$hintDigit([^0-9]|$)").containsMatchIn(name)
            }
            if (hinted != null && remainingCandidates.count { it === hinted } == 1) {
                remainingCandidates.remove(hinted)
                val created = copyInto(hinted, dsiDir, slot.canonicalFileName)
                results += if (created) {
                    SlotResult(slot, Outcome.CREATED, listOf(hinted.name.orEmpty()), resolvedByFileNameHint = true)
                } else {
                    SlotResult(slot, Outcome.MISSING)
                }
            } else {
                results += SlotResult(slot, Outcome.AMBIGUOUS, candidates.mapNotNull { it.name })
            }
        }
        results += presentSlotResults(bios7Present, bios9Present)
        return results
    }

    private fun presentSlotResults(bios7Present: Boolean, bios9Present: Boolean): List<SlotResult> {
        val results = mutableListOf<SlotResult>()
        if (bios7Present) results += SlotResult(BiosSlot.DSI_BIOS7, Outcome.ALREADY_PRESENT)
        if (bios9Present) results += SlotResult(BiosSlot.DSI_BIOS9, Outcome.ALREADY_PRESENT)
        return results
    }

    private fun resolveNand(dsiDir: DocumentFile?, candidate: DocumentFile?): NandResult {
        if (dsiDir == null) {
            return NandResult(Outcome.MISSING)
        }
        if (dsiDir.findFile(NAND_FILE_NAME) != null) {
            return NandResult(Outcome.ALREADY_PRESENT)
        }
        if (candidate == null) {
            return NandResult(Outcome.MISSING)
        }
        return if (copyInto(candidate, dsiDir, NAND_FILE_NAME)) {
            NandResult(Outcome.CREATED, candidate.name)
        } else {
            NandResult(Outcome.MISSING)
        }
    }

    private fun copyInto(source: DocumentFile, destDir: DocumentFile, destName: String): Boolean {
        val dest = destDir.createFile("application/octet-stream", destName) ?: return false
        return try {
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                context.contentResolver.openOutputStream(dest.uri)?.use { output ->
                    input.copyTo(output)
                } ?: return false
            } ?: return false
            true
        } catch (e: IOException) {
            // Clean up the partial file we just created; never touch the source.
            dest.delete()
            false
        }
    }

    private fun getFileSize(file: DocumentFile): Long? {
        return try {
            context.contentResolver.openAssetFileDescriptor(file.uri, "r")?.use {
                if (it.length == AssetFileDescriptor.UNKNOWN_LENGTH) null else it.length
            }
        } catch (e: FileNotFoundException) {
            null
        }
    }

    /**
     * Reads the byte at [FirmwareConsoleType.HEADER_OFFSET]. Returns null if it can't be read, in
     * which case the caller must not treat the file as matching either console.
     */
    private fun readFirmwareConsoleTypeByte(file: DocumentFile): Int? {
        return try {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                var skipped = 0L
                while (skipped < FirmwareConsoleType.HEADER_OFFSET) {
                    val n = stream.skip((FirmwareConsoleType.HEADER_OFFSET - skipped).toLong())
                    if (n <= 0) return null
                    skipped += n
                }
                val value = stream.read()
                if (value == -1) null else value
            }
        } catch (e: IOException) {
            null
        }
    }

    companion object {
        const val DS_FOLDER_NAME = "DS"
        const val DSI_FOLDER_NAME = "DSi"
        const val NAND_FILE_NAME = "nand.bin"

        private val FIRMWARE_SIZES = (BiosSlot.DS_FIRMWARE.expectedSizeBytes.toList() + BiosSlot.DSI_FIRMWARE.expectedSizeBytes.toList()).distinct()
    }
}
