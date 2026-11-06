package me.magnum.melonds.common.romprocessors

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import android.util.Log
import androidx.core.content.getSystemService
import me.magnum.melonds.R
import me.magnum.melonds.common.uridelegates.UriHandler
import me.magnum.melonds.domain.model.SizeUnit
import me.magnum.melonds.impl.NdsRomCache
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream

class SevenZRomFileProcessor(private val context: Context, private val uriHandler: UriHandler, ndsRomCache: NdsRomCache) : CompressedRomFileProcessor(context, uriHandler, ndsRomCache) {

    companion object {
        private const val TAG = "SevenZRomFileProcessor"
        private const val OUT_OF_MEMORY_NOTIFICATION_COOLDOWN_MS = 10000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val outOfMemoryNotificationTimes = mutableMapOf<Uri, Long>()

    override fun getNdsEntryStreamInFileStream(fileStream: InputStream, romUri: Uri): RomFileStream? {
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
            val entry = getNdsEntryInFile(sevenZFile)
            if (entry == null) {
                closeQuietly(sevenZFile)
                null
            } else {
                val entryStream = object : FilterInputStream(sevenZFile.getInputStream(entry)) {
                    override fun close() {
                        try {
                            super.close()
                        } finally {
                            sevenZFile.close()
                        }
                    }
                }
                RomFileStream(entryStream, SizeUnit.Bytes(entry.size))
            }
        } catch (e: OutOfMemoryError) {
            // Last resort: the memory limit above should turn oversized dictionaries into a MemoryLimitException, but
            // a decoder allocation can still fail when the heap is already under pressure. Skip the ROM instead of
            // taking the whole process down with it.
            closeQuietly(sevenZFile)
            Log.w(TAG, "Not enough memory to read 7z ROM contents, skipping file", e)
            notificationOrNull { notifyOutOfMemory(romUri) }
            null
        } catch (e: Exception) {
            closeQuietly(sevenZFile)
            throw e
        }
    }

    /**
     * Informs the user that the given file could not be extracted due to a lack of memory. Since the same file may be processed multiple times in quick
     * succession (to fetch its metadata, icon, etc.), notifications for the same file are throttled to avoid spamming the user.
     */
    private fun notifyOutOfMemory(romUri: Uri) {
        val now = SystemClock.elapsedRealtime()
        synchronized(outOfMemoryNotificationTimes) {
            val lastNotificationTime = outOfMemoryNotificationTimes[romUri]
            if (lastNotificationTime != null && now - lastNotificationTime < OUT_OF_MEMORY_NOTIFICATION_COOLDOWN_MS) {
                return
            }
            outOfMemoryNotificationTimes[romUri] = now
        }

        val fileName = notificationOrNull { uriHandler.getUriDocument(romUri)?.name } ?: romUri.lastPathSegment.orEmpty()
        mainHandler.post {
            notificationOrNull { Toast.makeText(context, context.getString(R.string.error_rom_out_of_memory, fileName), Toast.LENGTH_LONG).show() }
        }
    }

    private inline fun <T> notificationOrNull(block: () -> T): T? = try {
        block()
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }

    private fun closeQuietly(sevenZFile: SevenZFile?) {
        try {
            sevenZFile?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close 7z file after error", e)
        }
    }

    private fun getNdsEntryInFile(sevenZFile: SevenZFile): SevenZArchiveEntry? {
        var entries = 0
        var precedingBytes = 0L
        do {
            val nextEntry = sevenZFile.nextEntry ?: break
            if (++entries > ArchiveReadBudget.MAX_ENTRIES) return null
            if (!nextEntry.isDirectory && isSupportedRomFile(nextEntry.name)) {
                if (nextEntry.size < 0 || nextEntry.size > ArchiveReadBudget.MAX_ROM_BYTES) return null
                return nextEntry
            }
            if (!nextEntry.isDirectory && nextEntry.size > 0) {
                precedingBytes += nextEntry.size
                if (precedingBytes > ArchiveReadBudget.MAX_DISCOVERY_BYTES) return null
            }
        } while (true)
        return null
    }
}
