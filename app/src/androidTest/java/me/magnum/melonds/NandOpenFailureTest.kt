package me.magnum.melonds

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Executes the actual JNI and Android file paths using only disposable fake inputs. */
@RunWith(AndroidJUnit4::class)
class NandOpenFailureTest {
    private fun openWithBiosLength(length: Int, expectedError: Int) = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MelonDSApplication>()
        val directory = File(app.cacheDir, "nand-failure-${System.nanoTime()}").apply { mkdirs() }
        try {
            val bios = File(directory, "bios7.bin").apply { writeBytes(ByteArray(length)) }
            val configuration = app.settingsRepository.getEmulatorConfiguration().copy(
                dsiBios7Uri = Uri.parse(bios.absolutePath),
                dsiNandUri = Uri.parse(File(directory, "missing-nand.bin").absolutePath),
            )
            assertEquals(expectedError, MelonDSiNand.openNand(configuration))
            // A failed open must not mark native state as already open.
            assertEquals(expectedError, MelonDSiNand.openNand(configuration))
        } finally {
            MelonDSiNand.closeNand()
            directory.deleteRecursively()
        }
    }

    @Test fun missingNandReportsFailure() = openWithBiosLength(0x8308 + 16, 3)
    @Test fun truncatedBiosReportsFailureBeforeReadingKey() = openWithBiosLength(8, 2)
}
