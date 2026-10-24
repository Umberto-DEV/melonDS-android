package androidx.documentfile.provider

import android.net.Uri

/** File-backed provider behavior: deletion fails, and rename support is selectable. */
internal class RestrictedDocument(
    private val delegate: DocumentFile,
    private val allowRename: Boolean = true,
) : DocumentFile(null) {
    private fun wrap(document: DocumentFile?) = document?.let { RestrictedDocument(it, allowRename) }
    override fun createFile(mimeType: String, displayName: String) = wrap(delegate.createFile(mimeType, displayName))
    override fun createDirectory(displayName: String) = wrap(delegate.createDirectory(displayName))
    override fun getUri(): Uri = delegate.uri
    override fun getName() = delegate.name
    override fun getType() = delegate.type
    override fun isDirectory() = delegate.isDirectory
    override fun isFile() = delegate.isFile
    override fun isVirtual() = delegate.isVirtual
    override fun lastModified() = delegate.lastModified()
    override fun length() = delegate.length()
    override fun canRead() = delegate.canRead()
    override fun canWrite() = delegate.canWrite()
    override fun delete() = false
    override fun exists() = delegate.exists()
    override fun listFiles(): Array<DocumentFile> = delegate.listFiles().map { RestrictedDocument(it, allowRename) }.toTypedArray()
    override fun renameTo(displayName: String) = allowRename && delegate.renameTo(displayName)
}
