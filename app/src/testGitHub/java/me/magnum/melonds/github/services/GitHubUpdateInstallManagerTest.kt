package me.magnum.melonds.github.services

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Component callbacks only: DownloadManager is shadowed; no network or APK installation occurs. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 33])
class GitHubUpdateInstallManagerTest {
    private lateinit var app: Application
    private lateinit var downloads: DownloadManager
    private val action get() = "${app.packageName}.INSTALL_RESULT"

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        downloads = app.getSystemService<DownloadManager>()!!
        if (Build.VERSION.SDK_INT == 28) {
            val permission = "${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            val info = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            val declared = info.permissions.orEmpty().single { it.name == permission }
            assertEquals(PermissionInfo.PROTECTION_SIGNATURE, declared.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE)
            assertTrue(permission in info.requestedPermissions.orEmpty())
            // Android grants an app its own declared signature permission; this plain Application fixture does not.
            shadowOf(app).grantPermissions(permission)
            assertEquals(PackageManager.PERMISSION_GRANTED, app.checkSelfPermission(permission))
        }
    }

    private fun deliver(status: Int, id: Long, confirmation: Intent? = null) {
        val intent = Intent(action).putExtra(PackageInstaller.EXTRA_STATUS, status).putExtra("download_id", id)
        confirmation?.let { intent.putExtra(Intent.EXTRA_INTENT, it) }
        shadowOf(app).getReceiversForIntent(Intent(action)).single().onReceive(app, intent)
    }

    @Test
    fun receiversRemainCompatibleAndInstallResultsAreNotExported() {
        GitHubUpdateInstallManager(app)
        val registered = shadowOf(app).registeredReceivers
        assertEquals(1, registered.count { it.intentFilter.hasAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE) })
        val result = registered.single { it.intentFilter.hasAction(action) }
        if (Build.VERSION.SDK_INT >= 33) {
            assertEquals(Context.RECEIVER_NOT_EXPORTED, result.flags and Context.RECEIVER_NOT_EXPORTED)
        }
    }

    @Test
    fun successfulInstallRemovesOnlyTheCorrespondingDownload() {
        GitHubUpdateInstallManager(app)
        val own = downloads.enqueue(DownloadManager.Request(Uri.parse("https://example.invalid/own.apk")))
        val other = downloads.enqueue(DownloadManager.Request(Uri.parse("https://example.invalid/other.apk")))
        deliver(PackageInstaller.STATUS_SUCCESS, own)
        assertNull(shadowOf(downloads).getRequest(own))
        assertNotNull(shadowOf(downloads).getRequest(other))
    }

    @Test
    fun failedOrCancelledInstallDoesNotDeleteTheDownloadedFile() {
        GitHubUpdateInstallManager(app)
        val own = downloads.enqueue(DownloadManager.Request(Uri.parse("https://example.invalid/own.apk")))
        for (status in listOf(PackageInstaller.STATUS_FAILURE, PackageInstaller.STATUS_FAILURE_ABORTED)) {
            deliver(status, own)
            assertNotNull(shadowOf(downloads).getRequest(own))
        }
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun pendingUserActionLaunchesTheConfirmationInANewTask() {
        GitHubUpdateInstallManager(app)
        val confirmation = Intent("test.confirm.install").setPackage(app.packageName)
        deliver(PackageInstaller.STATUS_PENDING_USER_ACTION, 7L, confirmation)
        val launched = shadowOf(app).nextStartedActivity
        assertEquals(confirmation.action, launched.action)
        assertEquals(app.packageName, launched.`package`)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, launched.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
