package com.hirden.flomofloat

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDownloadManager
import org.robolectric.shadows.ShadowSigningInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateStorageTest {
    private lateinit var context: Context
    private lateinit var release: AppRelease
    private lateinit var file: File
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().clear().commit()
        val folder = File(context.filesDir, "updates").apply { deleteRecursively(); mkdirs() }
        val temp = File(folder, "fixture").apply { writeText("APK fixture bytes") }
        release = AppRelease(BuildConfig.VERSION_CODE + 1, "next", "https://example.org/rumo.apk", AppUpdates.sha256(temp), "说明", 26)
        file = File(folder, "rumo-${release.code}-${release.sha256}.apk")
        temp.renameTo(file)
        context.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putString("release", release.json()).commit()
        val pm = shadowOf(context.packageManager)
        pm.getInternalMutablePackageInfo(context.packageName).signingInfo = signing("trusted")
        pm.setPackageArchiveInfo(file.path, packageInfo(context.packageName, "trusted"))
    }
    private fun signing(value: String): SigningInfo = Shadow.newInstanceOf(SigningInfo::class.java).also {
        Shadow.extract<ShadowSigningInfo>(it).setSignatures(arrayOf(Signature(value.toByteArray())))
    }
    @Suppress("DEPRECATION")
    private fun packageInfo(name: String, signature: String) = PackageInfo().apply {
        packageName = name; versionCode = BuildConfig.VERSION_CODE + 1; signingInfo = signing(signature)
    }
    private fun updates(): AppUpdates = AppUpdates::class.java.getDeclaredConstructor(Context::class.java).apply { isAccessible = true }.newInstance(context)
    private fun awaitWorker(updates: AppUpdates) {
        val worker = AppUpdates::class.java.getDeclaredField("worker").apply { isAccessible = true }.get(updates) as ExecutorService
        worker.submit {}.get(10, TimeUnit.SECONDS)
    }
    @Test fun builtInUpdateChannelIsUsedAndCustomSourceSurvivesReopening() {
        val updates = updates()
        assertEquals(UpdateChannel.DEFAULT_FEED_URL, updates.feed)
        updates.configure("https://example.org/my-updates.json", false)
        assertFalse("Saving settings must not start a network check", updates.working)
        assertEquals("https://example.org/my-updates.json", updates().feed)
        assertFalse(updates().autoCheck)
        updates.configure("", false)
        assertEquals(UpdateChannel.DEFAULT_FEED_URL, updates().feed)
    }
    @Test fun verifiedDownloadIsReusedAfterReopening() {
        val updates = updates()
        updates.refreshDownload(); awaitWorker(updates)
        assertTrue(updates.downloaded)
        assertEquals(file, updates.installerFile())
        repeat(3) { updates.download() }; awaitWorker(updates)
        assertEquals(0, shadowOf(context.getSystemService(DownloadManager::class.java)).requestCount)
        val reopened = updates()
        reopened.refreshDownload(); awaitWorker(reopened)
        assertTrue(reopened.downloaded)
    }
    @Test fun repeatedDownloadAndReopeningReuseExistingSystemTask() {
        file.delete()
        val updates = updates()
        updates.download(); awaitWorker(updates)
        val manager = context.getSystemService(DownloadManager::class.java)
        val shadow = shadowOf(manager)
        val id = updates.downloadId
        assertTrue(id >= 0)
        Shadow.extract<ShadowDownloadManager.ShadowRequest>(shadow.getRequest(id)).setStatus(DownloadManager.STATUS_RUNNING)
        updates.download(); awaitWorker(updates)
        val reopened = updates()
        reopened.download(); awaitWorker(reopened)
        assertEquals(id, reopened.downloadId)
        assertEquals(1, shadow.requestCount)
    }
    @Test fun wrongSignatureIsNotInstallable() {
        shadowOf(context.packageManager).setPackageArchiveInfo(file.path, packageInfo(context.packageName, "untrusted"))
        val updates = updates()
        updates.refreshDownload(); awaitWorker(updates)
        assertFalse(updates.downloaded); assertNull(updates.installerFile()); assertFalse(file.exists())
    }
    @Test fun wrongHashIsNotInstallable() {
        file.writeText("tampered bytes")
        val updates = updates()
        updates.refreshDownload(); awaitWorker(updates)
        assertFalse(updates.downloaded); assertNull(updates.installerFile())
    }
}
