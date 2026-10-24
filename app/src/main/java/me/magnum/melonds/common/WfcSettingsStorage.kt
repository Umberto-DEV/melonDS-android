package me.magnum.melonds.common

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Storage operations shared by WFC settings screens. */
internal object WfcSettingsStorage {
    // Shared across screen instances: the native editor reads and rewrites a whole image.
    private val storageMutex = Mutex()

    suspend fun <T> withTransaction(operation: suspend () -> T): T = storageMutex.withLock { operation() }

    fun ensureFirmwareBackup(
        context: Context,
        directory: DocumentFile,
        source: Uri,
        fileName: String,
    ): Boolean {
        var incompleteBackup: DocumentFile? = null
        var completed = false
        return try {
            val backupName = "$fileName.bak"
            val existing = directory.findFile(backupName)
            if (existing != null && existing.length() > 0) {
                // Keep the original even if this firmware has since been edited. Check the
                // actual target each time, including after a directory change or backup deletion.
                return true
            }
            if (existing != null && !existing.delete()) return false

            // A failed copy may be undeletable. Keep it under a candidate name that is never
            // accepted as the original backup; a retry opens it with truncation and starts over.
            val candidateName = "$backupName.partial"
            val backup = directory.findFile(candidateName)
                ?: directory.createFile("application/octet-stream", candidateName)
                ?: return false
            incompleteBackup = backup
            context.contentResolver.openInputStream(source)?.use { input ->
                context.contentResolver.openOutputStream(backup.uri, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: return false
            } ?: return false
            // Both streams have closed successfully. Providers without rename support fail
            // safely here, before the caller is allowed to modify the source firmware.
            if (directory.findFile(backupName) != null || !backup.renameTo(backupName)) return false
            completed = true
            true
        } catch (error: Exception) {
            Log.w("WfcConnections", "Failed to back up the firmware file", error)
            false
        } finally {
            if (!completed) {
                incompleteBackup?.let { backup ->
                    if (!runCatching { backup.delete() }.getOrDefault(false)) {
                        Log.w("WfcConnections", "Failed to remove incomplete firmware backup")
                    }
                }
            }
        }
    }
}
