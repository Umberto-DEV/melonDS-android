package me.magnum.melonds.common.romprocessors

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import me.magnum.melonds.common.uridelegates.UriHandler
import me.magnum.melonds.domain.model.SizeUnit
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.domain.model.rom.config.RomConfig
import me.magnum.melonds.domain.repositories.SettingsRepository
import me.magnum.melonds.impl.NdsRomCache
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Compressed ROMs allocate large decoder buffers on the Java heap, so the archives must be decoded one at a time
 * across the whole process, whichever component (ROM scan, icon extraction, ROM info) asks for them. This used to be
 * enforced by routing every ROM file access through one single-threaded dispatcher, which also queued the icons of
 * the ROM list behind the directory scan; the bound now lives in [CompressedRomFileProcessor] itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class CompressedRomFileProcessorTest {

    private lateinit var context: Context
    private lateinit var romUri: Uri

    private val decodersOpen = AtomicInteger(0)
    private val maxDecodersOpen = AtomicInteger(0)

    /** Archive decoder stand-in: reports how many entries are being decoded at the same time. */
    private inner class RecordingProcessor(ndsRomCache: NdsRomCache) : CompressedRomFileProcessor(context, notNeeded(), ndsRomCache) {
        override fun getNdsEntryStreamInFileStream(fileStream: InputStream): RomFileStream {
            val open = decodersOpen.incrementAndGet()
            maxDecodersOpen.accumulateAndGet(open) { a, b -> maxOf(a, b) }
            Thread.sleep(150)
            val entryStream = object : ByteArrayInputStream(ByteArray(0x1000)) {
                override fun close() {
                    decodersOpen.decrementAndGet()
                    super.close()
                }
            }
            return RomFileStream(entryStream, SizeUnit.Bytes(0x1000))
        }
    }

    private inline fun <reified T> notNeeded(): T {
        return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            throw UnsupportedOperationException("${method.name} is not needed for this test")
        } as T
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val romFile = File(context.cacheDir, "game.zip").apply { writeBytes(ByteArray(64)) }
        romUri = Uri.fromFile(romFile)
    }

    @Test
    fun `archives are decoded one at a time across processors`() {
        val ndsRomCache = NdsRomCache(context, notNeeded<SettingsRepository>())
        val processors = List(3) { RecordingProcessor(ndsRomCache) }
        val rom = Rom("Game", "Dev", "game.zip", romUri, null, RomConfig.default(), null, false, "")

        val executor = Executors.newFixedThreadPool(processors.size)
        val allDone = CountDownLatch(processors.size)
        processors.forEach { processor ->
            executor.execute {
                try {
                    processor.getRomIcon(rom)
                } finally {
                    allDone.countDown()
                }
            }
        }
        allDone.await(10, TimeUnit.SECONDS)
        executor.shutdownNow()

        assertEquals("Archives decoded at the same time", 1, maxDecodersOpen.get())
        assertEquals("Decoders left open", 0, decodersOpen.get())
    }
}
