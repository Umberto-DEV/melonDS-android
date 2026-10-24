package me.magnum.melonds.common.romprocessors

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import me.magnum.melonds.common.uridelegates.UriHandler
import me.magnum.melonds.domain.model.SizeUnit
import me.magnum.melonds.impl.NdsRomCache
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.FileInputStream
import java.io.InputStream

class SevenZRomFileProcessor(private val context: Context, uriHandler: UriHandler, ndsRomCache: NdsRomCache) : CompressedRomFileProcessor(context, uriHandler, ndsRomCache) {

    companion object {
        private const val TAG = "SevenZRomFileProcessor"
    }

    override fun getNdsEntryStreamInFileStream(fileStream: InputStream): RomFileStream? {
        if (fileStream !is FileInputStream) {
            return null
        }

        val deviceMemory = context.getSystemService<ActivityManager>()?.let {
            val memoryInfo = ActivityManager.MemoryInfo()
            it.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem
        } ?: Int.MAX_VALUE.toLong()

        var sevenZFile: SevenZFile? = null
        return try {
            sevenZFile = SevenZFile.Builder()
                .setMaxMemoryLimitKb(SevenZMemoryLimit.kb(deviceMemory, Runtime.getRuntime().maxMemory()))
                .setSeekableByteChannel(fileStream.channel)
                .get()
            getNdsEntryInFile(sevenZFile)?.let {
                RomFileStream(sevenZFile.getInputStream(it), SizeUnit.Bytes(it.size))
            }
        } catch (e: OutOfMemoryError) {
            // Last resort: the memory limit above should turn oversized dictionaries into a MemoryLimitException, but
            // a decoder allocation can still fail when the heap is already under pressure. Skip the ROM instead of
            // taking the whole process down with it.
            closeQuietly(sevenZFile)
            Log.w(TAG, "Not enough memory to read 7z ROM contents, skipping file", e)
            null
        } catch (e: Exception) {
            closeQuietly(sevenZFile)
            throw e
        }
    }

    private fun closeQuietly(sevenZFile: SevenZFile?) {
        try {
            sevenZFile?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close 7z file after error", e)
        }
    }

    private fun getNdsEntryInFile(sevenZFile: SevenZFile): SevenZArchiveEntry? {
        do {
            val nextEntry = sevenZFile.nextEntry ?: break
            if (!nextEntry.isDirectory && isSupportedRomFile(nextEntry.name)) {
                return nextEntry
            }
        } while (true)
        return null
    }
}
