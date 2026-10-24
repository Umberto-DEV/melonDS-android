package me.magnum.melonds.impl.romprocessors

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import me.magnum.melonds.common.romprocessors.RomFileProcessor
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BaseRomFileProcessorFactoryTest {

    private val ndsProcessor = object : RomFileProcessor {
        override fun getRomFromUri(romUri: Uri, parentUri: Uri?) = null
        override fun getRomIcon(rom: me.magnum.melonds.domain.model.rom.Rom) = null
        override fun getRomInfo(rom: me.magnum.melonds.domain.model.rom.Rom) = null
        override suspend fun getRealRomUri(rom: me.magnum.melonds.domain.model.rom.Rom) = null
    }
    private val sevenZProcessor = object : RomFileProcessor {
        override fun getRomFromUri(romUri: Uri, parentUri: Uri?) = null
        override fun getRomIcon(rom: me.magnum.melonds.domain.model.rom.Rom) = null
        override fun getRomInfo(rom: me.magnum.melonds.domain.model.rom.Rom) = null
        override suspend fun getRealRomUri(rom: me.magnum.melonds.domain.model.rom.Rom) = null
    }

    private val factory = object : BaseRomFileProcessorFactory(ApplicationProvider.getApplicationContext()) {
        override fun getRomFileProcessorForFileExtension(extension: String): RomFileProcessor? {
            return when (extension) {
                "nds" -> ndsProcessor
                "7z" -> sevenZProcessor
                else -> null
            }
        }
    }

    @Test
    fun `file scheme uri resolves processor from its path name`() {
        val uri = Uri.parse("file:///sdcard/roms/game.nds")

        val processor = factory.getFileRomProcessorForDocument(uri)

        assertSame(ndsProcessor, processor)
    }

    @Test
    fun `content uri without document name falls back to last path segment`() {
        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3Aroms%2Fgame.7z")

        // Robolectric's shadow DocumentFile resolves no name for this provider (no ContentResolver
        // registered), so the fallback to `lastPathSegment` ("primary:roms/game.7z") is exercised.
        val processor = factory.getFileRomProcessorForDocument(uri)

        assertSame(sevenZProcessor, processor)
    }

    @Test
    fun `uri without an extension resolves to no processor`() {
        val uri = Uri.parse("file:///sdcard/roms/game")

        val processor = factory.getFileRomProcessorForDocument(uri)

        assertNull(processor)
    }
}
