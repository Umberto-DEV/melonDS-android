package me.magnum.melonds.impl

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import me.magnum.melonds.domain.model.*
import me.magnum.melonds.domain.model.camera.DSiCameraSourceType
import me.magnum.melonds.domain.model.input.SoftInputBehaviour
import me.magnum.melonds.domain.model.render.RenderStrategy
import me.magnum.melonds.domain.model.rewind.RewindWindowPosition
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.domain.repositories.SettingsRepository
import me.magnum.melonds.ui.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

/**
 * Minimal [SettingsRepository] stub. Only [reloadControllerConfiguration] is exercised by
 * [SettingsBackupManager]; every other member is unused by the manager and throws if called.
 */
private class FakeSettingsRepository : SettingsRepository {
    var reloadCalled = false
        private set

    private fun notNeeded(): Nothing = throw UnsupportedOperationException("Not needed for this test")

    override suspend fun getEmulatorConfiguration(): EmulatorConfiguration = notNeeded()
    override fun getTheme(): Theme = notNeeded()
    override fun getFastForwardSpeedMultiplier(): Float = notNeeded()
    override fun shouldMuteFastForwardAudio(): Boolean = notNeeded()
    override fun isRewindEnabled(): Boolean = notNeeded()
    override fun getRewindWindowPosition(): RewindWindowPosition = notNeeded()
    override fun isSustainedPerformanceModeEnabled(): Boolean = notNeeded()
    override fun getRomSearchDirectories(): Array<Uri> = notNeeded()
    override fun clearRomSearchDirectories() {}
    override fun getRomIconFiltering(): RomIconFiltering = notNeeded()
    override fun getRomCacheMaxSize(): SizeUnit = notNeeded()
    override fun getDefaultConsoleType(): ConsoleType = notNeeded()
    override fun getFirmwareConfiguration(): FirmwareConfiguration = notNeeded()
    override fun useCustomBios(): Boolean = notNeeded()
    override fun getDsBiosDirectory(): Uri? = notNeeded()
    override fun getDsiBiosDirectory(): Uri? = notNeeded()
    override fun showBootScreen(): Boolean = notNeeded()
    override fun isJitEnabled(): Boolean = notNeeded()
    override fun isRtcSyncToHostEnabled(): Boolean = notNeeded()
    override fun getVideoRenderer(): Flow<VideoRenderer> = notNeeded()
    override fun getVideoInternalResolutionScaling(): Flow<Int> = notNeeded()
    override fun getVideoFiltering(): Flow<VideoFiltering> = notNeeded()
    override fun isThreadedRenderingEnabled(): Flow<Boolean> = notNeeded()
    override fun getRenderStrategy(): Flow<RenderStrategy> = notNeeded()
    override fun getFpsCounterPosition(): FpsCounterPosition = notNeeded()
    override fun getDSiCameraSource(): DSiCameraSourceType = notNeeded()
    override fun getDSiCameraStaticImage(): Uri? = notNeeded()
    override fun isSoundEnabled(): Boolean = notNeeded()
    override fun getAudioLatency(): AudioLatency = notNeeded()
    override fun getMicSource(): MicSource = notNeeded()
    override fun getRomSortingMode(): SortingMode = notNeeded()
    override fun getRomSortingOrder(): SortingOrder = notNeeded()
    override fun saveNextToRomFile(): Boolean = notNeeded()
    override fun getSaveFileDirectory(): Uri? = notNeeded()
    override fun getSaveFileDirectory(rom: Rom): Uri = notNeeded()
    override fun getSaveStateLocation(rom: Rom): SaveStateLocation = notNeeded()
    override fun getSaveStateDirectory(rom: Rom): Uri? = notNeeded()
    override fun getControllerConfiguration(): ControllerConfiguration = notNeeded()
    override fun observeControllerConfiguration(): StateFlow<ControllerConfiguration> = notNeeded()
    override fun reloadControllerConfiguration() { reloadCalled = true }
    override fun getSelectedLayoutId(): UUID = notNeeded()
    override fun getSoftInputBehaviour(): Flow<SoftInputBehaviour> = notNeeded()
    override fun isTouchHapticFeedbackEnabled(): Flow<Boolean> = notNeeded()
    override fun getTouchHapticFeedbackStrength(): Int = notNeeded()
    override fun getSoftInputOpacity(): Flow<Int> = notNeeded()
    override fun isRetroAchievementsRichPresenceEnabled(): Boolean = notNeeded()
    override fun isRetroAchievementsHardcoreEnabled(): Boolean = notNeeded()
    override fun areRetroAchievementsActiveChallengeIndicatorsEnabled(): Boolean = notNeeded()
    override fun areRetroAchievementsProgressIndicatorsEnabled(): Boolean = notNeeded()
    override fun areRetroAchievementsLeaderboardIndicatorsEnabled(): Boolean = notNeeded()
    override fun areCheatsEnabled(): Boolean = notNeeded()
    override fun observeTheme(): Flow<Theme> = notNeeded()
    override fun observeRomIconFiltering(): Flow<RomIconFiltering> = notNeeded()
    override fun observeRomSearchDirectories(): Flow<Array<Uri>> = notNeeded()
    override fun observeSelectedLayoutId(): Flow<UUID> = notNeeded()
    override fun observeDSiCameraSource(): Flow<DSiCameraSourceType> = notNeeded()
    override fun observeDSiCameraStaticImage(): Flow<Uri?> = notNeeded()
    override fun setDsBiosDirectory(directoryUri: Uri) {}
    override fun setDsiBiosDirectory(directoryUri: Uri) {}
    override fun addRomSearchDirectory(directoryUri: Uri) {}
    override fun setControllerConfiguration(controllerConfiguration: ControllerConfiguration) {}
    override fun setRomSortingMode(sortingMode: SortingMode) {}
    override fun setRomSortingOrder(sortingOrder: SortingOrder) {}
    override fun setSelectedLayoutId(layoutId: UUID) {}
    override fun observeRenderConfiguration(): Flow<RendererConfiguration> = notNeeded()
}

/**
 * Exercises the backup/restore round trip using a plain, file-backed [DocumentFile]
 * (see [SettingsBackupManager.backupToDocument]/[restoreFromDocument]) instead of a real SAF
 * tree Uri, since a document tree provider isn't available under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsBackupManagerTest {

    private lateinit var context: Application
    private lateinit var preferences: SharedPreferences
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var manager: SettingsBackupManager
    private lateinit var backupRoot: DocumentFile

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        settingsRepository = FakeSettingsRepository()
        manager = SettingsBackupManager(context, preferences, settingsRepository)

        val backupDir = File(context.cacheDir, "backup_test_${System.nanoTime()}").apply { mkdirs() }
        backupRoot = DocumentFile.fromFile(backupDir)
    }

    @Test
    fun `backup then restore round trips preferences, controller config and layouts`() {
        preferences.edit()
            .putBoolean("enable_jit", true)
            .putString("theme", "dark")
            .putInt("fast_forward_speed_multiplier", 4)
            .commit()

        val controllerJson = """{"inputs":[{"key":"a"}]}"""
        File(context.filesDir, "controller_config.json").writeText(controllerJson)

        val layoutsJson = """[{"id":"11111111-1111-1111-1111-111111111111","name":"My layout"}]"""
        File(context.filesDir, "layouts.json").writeText(layoutsJson)

        manager.backupToDocument(backupRoot)

        assertTrue(File(backupRoot.uri.path!!, "settings.json").exists())
        assertTrue(File(backupRoot.uri.path!!, "controller_config.json").exists())
        assertTrue(File(backupRoot.uri.path!!, "layouts.json").exists())

        // Simulate a fresh install: wipe local state, then restore from the backup.
        preferences.edit().clear().commit()
        File(context.filesDir, "controller_config.json").delete()
        File(context.filesDir, "layouts.json").delete()

        manager.restoreFromDocument(backupRoot)

        assertEquals(true, preferences.getBoolean("enable_jit", false))
        assertEquals("dark", preferences.getString("theme", null))
        assertEquals(4, preferences.getInt("fast_forward_speed_multiplier", -1))
        assertEquals(controllerJson, File(context.filesDir, "controller_config.json").readText())
        assertEquals(layoutsJson, File(context.filesDir, "layouts.json").readText())
        assertTrue("restore should invalidate the in-memory controller config cache", settingsRepository.reloadCalled)
    }

    @Test
    fun `excluded preference keys are never overwritten by restore`() {
        preferences.edit().putString("ra_token", "original-secret").commit()

        manager.backupToDocument(backupRoot)
        // Simulate the token having changed locally after the backup was made.
        preferences.edit().putString("ra_token", "still-local-secret").commit()

        manager.restoreFromDocument(backupRoot)

        assertEquals("still-local-secret", preferences.getString("ra_token", null))
    }
}
