package me.magnum.melonds.impl

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.magnum.melonds.common.romprocessors.RomFileProcessor
import me.magnum.melonds.common.romprocessors.RomFileProcessorFactory
import me.magnum.melonds.di.MelonModule
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.domain.model.rom.config.RomConfig
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The ROM list asks [RomIconProvider] for one icon per visible row as soon as the cached ROM list is shown, while
 * [FileSystemRomsRepository] is still scanning the ROM directories on the ROM file access dispatcher. Icons that are
 * already in the disk cache must not wait for that scan to finish, otherwise every row stays blank (and the refresh
 * indicator keeps spinning) until the whole directory has been read.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class RomIconProviderTest {

    private companion object {
        /** How long the simulated directory scan keeps the ROM file access dispatcher busy. */
        const val SCAN_DURATION_MS = 3_000L
    }

    private lateinit var context: Context
    private lateinit var busyScope: CoroutineScope
    private val scanRunning = CountDownLatch(1)

    /** Never reached: the icon under test is served from the disk cache. */
    private val factory = object : RomFileProcessorFactory {
        override fun getFileRomProcessorForDocument(romDocument: DocumentFile): RomFileProcessor? = error("Icon must come from the disk cache")
        override fun getFileRomProcessorForDocument(romUri: Uri): RomFileProcessor? = error("Icon must come from the disk cache")
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        busyScope = CoroutineScope(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        busyScope.cancel()
    }

    @Test
    fun `cached icon is served while the ROM file access dispatcher is busy scanning`() = runBlocking {
        val rom = Rom("Game", "Dev", "game.nds", Uri.parse("content://roms/game.nds"), null, RomConfig.default(), null, false, "")
        writeCachedIcon(rom)

        // Wire the provider exactly as the app does, next to the singleton ROM file access dispatcher
        val romFileAccessDispatcher = MelonModule.provideRomFileAccessDispatcher()
        val provider = MelonModule.provideRomIconProvider(context, factory)

        // Occupy the (single-threaded) ROM file access dispatcher like a long directory scan would
        busyScope.launch(romFileAccessDispatcher) {
            scanRunning.countDown()
            Thread.sleep(SCAN_DURATION_MS)
        }
        scanRunning.await(5, TimeUnit.SECONDS)

        val start = System.nanoTime()
        val icon = provider.getRomIcon(rom)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertNotNull("Cached icon was not served", icon)
        assertTrue("Cached icon took $elapsedMs ms: it waited for the ROM scan", elapsedMs < SCAN_DURATION_MS / 2)
    }

    private fun writeCachedIcon(rom: Rom) {
        val iconCacheDir = File(context.externalCacheDir!!, "rom_icons").apply { mkdirs() }
        val iconFile = File(iconCacheDir, rom.uri.hashCode().toString())
        iconFile.outputStream().use {
            Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
