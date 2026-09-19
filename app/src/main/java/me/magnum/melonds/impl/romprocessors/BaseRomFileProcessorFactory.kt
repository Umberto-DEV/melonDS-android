package me.magnum.melonds.impl.romprocessors

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import me.magnum.melonds.common.romprocessors.RomFileProcessor
import me.magnum.melonds.common.romprocessors.RomFileProcessorFactory
import java.io.File

abstract class BaseRomFileProcessorFactory(private val context: Context) : RomFileProcessorFactory {
    /**
     * Returns the [RomFileProcessor] to be used on files with the given [extension]. May return null if no processor exists that supports the given [extension].
     *
     * @param extension The extension (in lowercase) of the string for which the [RomFileProcessor] must be returned.
     * @return The [RomFileProcessor] to be used for the given [extension]. May be null if there's no suitable [RomFileProcessor].
     */
    abstract fun getRomFileProcessorForFileExtension(extension: String): RomFileProcessor?

    override fun getFileRomProcessorForDocument(romDocument: DocumentFile): RomFileProcessor? {
        return getRomFileProcessorForFileName(romDocument.name)
    }

    override fun getFileRomProcessorForDocument(romUri: Uri): RomFileProcessor? {
        return getRomFileProcessorForFileName(getFileNameForUri(romUri))
    }

    private fun getRomFileProcessorForFileName(fileName: String?): RomFileProcessor? {
        if (fileName == null) return null
        val lastDotIndex = fileName.lastIndexOf('.')
        if (lastDotIndex < 0) return null

        val extension = fileName.substring(lastDotIndex + 1).lowercase()
        return getRomFileProcessorForFileExtension(extension)
    }

    // "file://" URIs (e.g. from external file managers) and providers that don't implement
    // DocumentsContract queries resolve to a null DocumentFile name, so fall back accordingly.
    private fun getFileNameForUri(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).name }
        }
        return DocumentFile.fromSingleUri(context, uri)?.name
            ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
    }
}