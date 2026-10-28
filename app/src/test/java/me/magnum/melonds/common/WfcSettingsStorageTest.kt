package me.magnum.melonds.common

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.RestrictedDocument
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class WfcSettingsStorageTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val context by lazy { ApplicationProvider.getApplicationContext<Application>() }

    @Test
    fun `overlapping file transactions retain both edits`() = runTest {
        val image = temporaryFolder.newFile("settings.bin").apply { writeText("0") }
        List(2) {
            launch {
                WfcSettingsStorage.withTransaction {
                    val previous = image.readText().toInt()
                    yield() // Both writers would read 0 without transaction serialization.
                    image.writeText((previous + 1).toString())
                }
            }
        }.joinAll()
        assertEquals("2", image.readText())
    }

    @Test
    fun `a different firmware directory receives its own original backup`() {
        val first = firmware("first", byteArrayOf(1, 2, 3))
        val second = firmware("second", byteArrayOf(4, 5, 6))
        assertTrue(backUp(first))
        assertTrue(backUp(second))
        assertArrayEquals(first.readBytes(), backup(first).readBytes())
        assertArrayEquals(second.readBytes(), backup(second).readBytes())
    }

    @Test
    fun `a missing backup is recreated even after an earlier successful backup`() {
        val source = firmware("deleted", byteArrayOf(1, 2, 3))
        assertTrue(backUp(source))
        assertTrue(backup(source).delete())
        assertTrue(backUp(source))
        assertArrayEquals(source.readBytes(), backup(source).readBytes())
    }

    @Test
    fun `an existing original backup survives later firmware edits`() {
        val original = byteArrayOf(1, 2, 3)
        val source = firmware("preserved", original)
        assertTrue(backUp(source))
        source.writeBytes(byteArrayOf(4, 5, 6))
        assertTrue(backUp(source))
        assertArrayEquals(original, backup(source).readBytes())
    }

    @Test
    fun `a failed copy removes a partial candidate so the next attempt can back up the original`() {
        for (alreadyEmpty in listOf(false, true)) {
            val source = firmware("failed-$alreadyEmpty", byteArrayOf(1, 2, 3))
            if (alreadyEmpty) backup(source).createNewFile()
            val sourceUri = DocumentFile.fromFile(source).uri
            Shadows.shadowOf(context.contentResolver).registerInputStream(sourceUri, object : InputStream() {
                private var started = false
                override fun read(): Int = throw IOException("copy interrupted")
                override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                    if (started) throw IOException("copy interrupted")
                    started = true
                    bytes[offset] = 1
                    return 1
                }
            })
            assertFalse(backUp(source))
            assertFalse("a partial backup must not be accepted on the next attempt", backup(source).exists())
            Shadows.shadowOf(context.contentResolver).registerInputStream(sourceUri, ByteArrayInputStream(source.readBytes()))
            assertTrue(backUp(source))
            assertArrayEquals(source.readBytes(), backup(source).readBytes())
        }
    }

    @Test
    fun `an undeletable failed candidate is never accepted as an original backup on retry`() {
        val source = firmware("undeletable", byteArrayOf(1, 2, 3))
        val directory = RestrictedDocument(DocumentFile.fromFile(source.parentFile!!))
        val sourceUri = DocumentFile.fromFile(source).uri
        Shadows.shadowOf(context.contentResolver).registerInputStream(sourceUri, object : InputStream() {
            private var started = false
            override fun read(): Int = throw IOException("copy interrupted")
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                if (started) throw IOException("copy interrupted")
                started = true
                bytes[offset] = 1
                return 1
            }
        })
        assertFalse(backUp(source, directory))
        assertFalse("a failed copy cannot become the original backup", backup(source).exists())
        val candidate = File(source.parentFile, "${source.name}.bak.partial")
        assertTrue("the provider refused to delete the partial candidate", candidate.length() > 0)

        Shadows.shadowOf(context.contentResolver).registerInputStream(sourceUri, ByteArrayInputStream(source.readBytes()))
        assertTrue(backUp(source, directory))
        assertArrayEquals(byteArrayOf(1, 2, 3), backup(source).readBytes())
        assertArrayEquals(byteArrayOf(1, 2, 3), source.readBytes())
    }

    @Test
    fun `a provider that cannot rename a complete candidate leaves the original firmware untouched`() {
        val source = firmware("no-rename", byteArrayOf(1, 2, 3))
        val directory = RestrictedDocument(DocumentFile.fromFile(source.parentFile!!), allowRename = false)
        assertFalse(backUp(source, directory))
        assertFalse(backup(source).exists())
        assertArrayEquals(byteArrayOf(1, 2, 3), source.readBytes())
    }

    private fun firmware(directory: String, bytes: ByteArray): File =
        File(temporaryFolder.newFolder(directory), "firmware.bin").apply { writeBytes(bytes) }

    private fun backup(source: File) = File(source.parentFile, "${source.name}.bak")

    private fun backUp(source: File, directory: DocumentFile = DocumentFile.fromFile(source.parentFile!!)): Boolean = WfcSettingsStorage.ensureFirmwareBackup(
        context, directory, DocumentFile.fromFile(source).uri,
        source.name,
    )

}
