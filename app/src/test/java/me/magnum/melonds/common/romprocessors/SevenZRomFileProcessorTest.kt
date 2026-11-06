package me.magnum.melonds.common.romprocessors

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import me.magnum.melonds.common.uridelegates.UriHandler
import me.magnum.melonds.domain.repositories.SettingsRepository
import me.magnum.melonds.impl.NdsRomCache
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Proxy
import java.nio.channels.FileChannel
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 35])
class SevenZRomFileProcessorTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        throw UnsupportedOperationException(method.name)
    } as T

    private fun processor(documentFailure: Throwable? = null): SevenZRomFileProcessor {
        val uris = Proxy.newProxyInstance(UriHandler::class.java.classLoader, arrayOf(UriHandler::class.java)) { _, method, _ ->
            if (method.name == "getUriDocument") {
                documentFailure?.let { throw it }
                null
            } else {
                throw UnsupportedOperationException(method.name)
            }
        } as UriHandler
        return SevenZRomFileProcessor(app, uris, NdsRomCache(app, unused<SettingsRepository>()))
    }

    @Test
    fun openingOutOfMemoryReportsTheSourceNameAndThrottlesOnlyTheSameUri() {
        ShadowToast.reset()
        val processor = processor()
        val file = File(app.cacheDir, "oom-source").apply { writeBytes(byteArrayOf(0)) }
        fun attempt(uri: Uri) {
            // Allocation failure is injected at the real decoder's channel boundary, without exhausting the heap.
            object : FileInputStream(file) {
                override fun getChannel(): FileChannel = throw OutOfMemoryError("Synthetic decoder-open failure")
            }.use { assertNull(processor.getNdsEntryStreamInFileStream(it, uri)) }
            shadowOf(Looper.getMainLooper()).idle()
        }
        val first = Uri.parse("content://test/first.7z")
        attempt(first)
        assertEquals(1, ShadowToast.shownToastCount())
        assertTrue(ShadowToast.getTextOfLatestToast().contains("first.7z"))
        attempt(first)
        assertEquals(1, ShadowToast.shownToastCount())
        attempt(Uri.parse("content://test/second.7z"))
        assertEquals(2, ShadowToast.shownToastCount())
        assertTrue(ShadowToast.getTextOfLatestToast().contains("second.7z"))
        ShadowSystemClock.advanceBy(Duration.ofSeconds(10))
        attempt(first)
        assertEquals(3, ShadowToast.shownToastCount())
    }

    private fun archive(name: String): File {
        val file = File(app.cacheDir, "synthetic-${name}.7z")
        SevenZOutputFile(file).use { output ->
            output.setContentMethods(listOf(SevenZMethodConfiguration(SevenZMethod.COPY)))
            val entry = SevenZArchiveEntry().apply { this.name = name; size = 3; isDirectory = false }
            output.putArchiveEntry(entry)
            output.write(byteArrayOf(1, 2, 3))
            output.closeArchiveEntry()
        }
        return file
    }

    @Test
    fun documentNameFailureStillReturnsNullAndShowsTheUriFallback() {
        ShadowToast.reset()
        val file = File(app.cacheDir, "oom-source").apply { writeBytes(byteArrayOf(0)) }
        for (failure in listOf(SecurityException("denied"), RuntimeException("closed provider"), OutOfMemoryError("name lookup"))) {
            val processor = processor(failure)
            val uri = Uri.parse("content://test/${failure.javaClass.simpleName}.7z")
            object : FileInputStream(file) {
                override fun getChannel(): FileChannel = throw OutOfMemoryError("Synthetic decoder-open failure")
            }.use { assertNull(processor.getNdsEntryStreamInFileStream(it, uri)) }
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(ShadowToast.getTextOfLatestToast().contains(uri.lastPathSegment!!))
        }
        assertEquals(3, ShadowToast.shownToastCount())
    }

    @Test
    fun anArchiveWithoutARomClosesTheDecoderChannel() {
        val file = archive("note.txt")
        FileInputStream(file).use { input ->
            assertNull(processor().getNdsEntryStreamInFileStream(input, Uri.fromFile(file)))
            assertFalse(input.channel.isOpen)
        }
    }

    @Test
    fun closingTheRomEntryAlsoClosesTheDecoderChannel() {
        val file = archive("synthetic.nds")
        FileInputStream(file).use { input ->
            val entry = processor().getNdsEntryStreamInFileStream(input, Uri.fromFile(file))
            assertNotNull(entry)
            entry!!.use { assertArrayEquals(byteArrayOf(1, 2, 3), it.readBytes()) }
            assertFalse(input.channel.isOpen)
        }
    }
}
