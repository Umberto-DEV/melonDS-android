package me.magnum.melonds.common

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import me.magnum.melonds.domain.model.BiosSlot
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.FirmwareConsoleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Exercises [BiosGuidedFolderSetup] against a real temp directory via [DocumentFile.fromFile],
 * which -- unlike [DocumentFile.fromTreeUri] -- needs no Storage Access Framework provider under
 * Robolectric (see [BiosGuidedFolderSetup.run] overload taking a [DocumentFile] directly).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BiosGuidedFolderSetupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context by lazy { ApplicationProvider.getApplicationContext<Application>() }
    private val setup by lazy { BiosGuidedFolderSetup(context) }

    @Test
    fun `full DS and DSi setup from arbitrarily named loose files creates folders and copies files under their canonical names`() {
        writeFile("random_bios_a.bin", 16384) // DS bios7
        writeFile("random_bios_b.bin", 4096) // DS bios9
        writeFile("random_firmware.bin", 262144, offset = FirmwareConsoleType.HEADER_OFFSET, value = 0xFF) // DS firmware
        writeFile("dsi_arm7_hint.bin", 65536) // DSi bios7/9 pair, resolved by name hint below
        writeFile("dsi_arm9_hint.bin", 65536)
        writeFile("dsi_firmware.bin", 131072, offset = FirmwareConsoleType.HEADER_OFFSET, value = FirmwareConsoleType.DSI_HEADER_BYTE)
        writeFile("nand.bin", 1024)

        val report = setup.run(DocumentFile.fromFile(tempFolder.root))

        assertNotNull(report.dsDirectory)
        assertNotNull(report.dsiDirectory)
        assertEquals("DS", report.dsDirectory?.name)
        assertEquals("DSi", report.dsiDirectory?.name)

        assertEquals(16384L, File(tempFolder.root, "DS/bios7.bin").length())
        assertEquals(4096L, File(tempFolder.root, "DS/bios9.bin").length())
        assertEquals(262144L, File(tempFolder.root, "DS/firmware.bin").length())
        assertEquals(65536L, File(tempFolder.root, "DSi/bios7.bin").length())
        assertEquals(65536L, File(tempFolder.root, "DSi/bios9.bin").length())
        assertEquals(131072L, File(tempFolder.root, "DSi/firmware.bin").length())
        assertTrue(File(tempFolder.root, "DSi/nand.bin").exists())

        assertSlot(report, BiosSlot.DS_BIOS7, BiosGuidedFolderSetup.Outcome.CREATED, listOf("random_bios_a.bin"))
        assertSlot(report, BiosSlot.DS_BIOS9, BiosGuidedFolderSetup.Outcome.CREATED, listOf("random_bios_b.bin"))
        assertSlot(report, BiosSlot.DS_FIRMWARE, BiosGuidedFolderSetup.Outcome.CREATED, listOf("random_firmware.bin"))
        assertSlot(report, BiosSlot.DSI_FIRMWARE, BiosGuidedFolderSetup.Outcome.CREATED, listOf("dsi_firmware.bin"))

        val bios7Result = report.slotResults.first { it.slot == BiosSlot.DSI_BIOS7 }
        assertEquals(BiosGuidedFolderSetup.Outcome.CREATED, bios7Result.outcome)
        assertEquals(listOf("dsi_arm7_hint.bin"), bios7Result.sourceFileNames)
        assertTrue(bios7Result.resolvedByFileNameHint)

        val bios9Result = report.slotResults.first { it.slot == BiosSlot.DSI_BIOS9 }
        assertEquals(BiosGuidedFolderSetup.Outcome.CREATED, bios9Result.outcome)
        assertEquals(listOf("dsi_arm9_hint.bin"), bios9Result.sourceFileNames)
        assertTrue(bios9Result.resolvedByFileNameHint)

        assertEquals(BiosGuidedFolderSetup.Outcome.CREATED, report.nandResult.outcome)
        assertEquals("nand.bin", report.nandResult.sourceFileName)
    }

    @Test
    fun `a source file that fails mid-copy leaves no partial destination file behind`() {
        val source = writeFile("broken_bios7.bin", 16384)
        val sourceUri = DocumentFile.fromFile(source).uri

        // The size read (openAssetFileDescriptor, used for classification) goes straight to the
        // real file and is unaffected; only the later read during the actual copy is poisoned --
        // this is what exercises copyInto's cleanup of the partially created destination file.
        Shadows.shadowOf(context.contentResolver).registerInputStream(sourceUri, object : InputStream() {
            override fun read(): Int = throw IOException("simulated failure mid-copy")
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("simulated failure mid-copy")
        })

        val report = setup.run(DocumentFile.fromFile(tempFolder.root))

        val result = report.slotResults.first { it.slot == BiosSlot.DS_BIOS7 }
        assertEquals(BiosGuidedFolderSetup.Outcome.MISSING, result.outcome)
        assertFalse("a 0-byte destination file must not be left behind", File(tempFolder.root, "DS/bios7.bin").exists())
    }

    @Test
    fun `two same-size DSi BIOS candidates both hinting at the same slot are AMBIGUOUS and nothing is copied`() {
        writeFile("dsi_bios_7_a.bin", 65536)
        writeFile("dsi_bios_7_b.bin", 65536)

        val report = setup.run(DocumentFile.fromFile(tempFolder.root))

        val bios7Result = report.slotResults.first { it.slot == BiosSlot.DSI_BIOS7 }
        assertEquals(BiosGuidedFolderSetup.Outcome.AMBIGUOUS, bios7Result.outcome)
        assertEquals(setOf("dsi_bios_7_a.bin", "dsi_bios_7_b.bin"), bios7Result.sourceFileNames.toSet())

        val bios9Result = report.slotResults.first { it.slot == BiosSlot.DSI_BIOS9 }
        assertEquals(BiosGuidedFolderSetup.Outcome.AMBIGUOUS, bios9Result.outcome)

        assertFalse(File(tempFolder.root, "DSi/bios7.bin").exists())
        assertFalse(File(tempFolder.root, "DSi/bios9.bin").exists())
    }

    @Test
    fun `an existing lowercase ds folder is reused instead of creating a second DS folder`() {
        tempFolder.newFolder("ds")
        writeFile("loose_bios7.bin", 16384)

        val report = setup.run(DocumentFile.fromFile(tempFolder.root))

        assertEquals("ds", report.dsDirectory?.name)
        val dsLikeFolders = tempFolder.root.listFiles { f -> f.isDirectory && f.name.equals("DS", ignoreCase = true) }.orEmpty()
        assertEquals(1, dsLikeFolders.size)
        assertTrue(File(tempFolder.root, "ds/bios7.bin").exists())
    }

    @Test
    fun `known limitation - a 256KB save file without the DSi header byte is classified as DS firmware regardless of its sav extension`() {
        // BiosFileClassifier only ever looks at size and (for firmware) the header byte at 0x1D;
        // the file's extension/name plays no part. A 256 KB .sav with no DSi signature therefore
        // matches DS_FIRMWARE exactly like a real firmware.bin of the same size would. This is a
        // documented, accepted limitation of content-only classification, not something this
        // change is meant to alter.
        writeFile("somebackup.sav", 262144)

        val report = setup.run(DocumentFile.fromFile(tempFolder.root))

        val result = report.slotResults.first { it.slot == BiosSlot.DS_FIRMWARE }
        assertEquals(BiosGuidedFolderSetup.Outcome.CREATED, result.outcome)
        assertEquals(listOf("somebackup.sav"), result.sourceFileNames)
        assertTrue(File(tempFolder.root, "DS/firmware.bin").exists())
    }

    private fun assertSlot(
        report: BiosGuidedFolderSetup.Report,
        slot: BiosSlot,
        outcome: BiosGuidedFolderSetup.Outcome,
        sourceFileNames: List<String>,
    ) {
        val result = report.slotResults.first { it.slot == slot }
        assertEquals(outcome, result.outcome)
        assertEquals(sourceFileNames, result.sourceFileNames)
        assertEquals(slot.consoleType, if (slot == BiosSlot.DS_BIOS7 || slot == BiosSlot.DS_BIOS9 || slot == BiosSlot.DS_FIRMWARE) ConsoleType.DS else ConsoleType.DSi)
    }

    private fun writeFile(name: String, size: Int, offset: Int? = null, value: Int? = null): File {
        val bytes = ByteArray(size)
        if (offset != null && value != null) {
            bytes[offset] = value.toByte()
        }
        val file = File(tempFolder.root, name)
        file.writeBytes(bytes)
        return file
    }
}
