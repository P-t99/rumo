package com.hirden.flomofloat

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

/** A public HTTPS manifest contains release metadata only, never the flomo API. */
data class AppRelease(val code: Int, val version: String, val url: String, val sha256: String, val notes: String, val minSdk: Int) {
    fun json(): String = JSONObject().put("versionCode", code).put("versionName", version).put("apkUrl", url)
        .put("sha256", sha256).put("notes", notes).put("minSdk", minSdk).toString()
    companion object {
        fun parse(raw: String): AppRelease {
            val value = JSONObject(raw)
            val code = value.getInt("versionCode")
            val name = value.getString("versionName")
            val url = value.getString("apkUrl")
            val sha = value.getString("sha256").lowercase()
            require(code > 0 && name.length in 1..40 && validHttps(url)) { "更新信息格式不正确" }
            require(sha.matches(Regex("[a-f0-9]{64}"))) { "更新文件缺少有效校验值" }
            val minSdk = value.optInt("minSdk", 26)
            require(minSdk >= 1)
            return AppRelease(code, name, url, sha, value.optString("notes").take(2000), minSdk)
        }
        fun validHttps(value: String): Boolean = runCatching {
            val uri = URI(value)
            uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null && uri.port in listOf(-1, 443)
        }.getOrDefault(false)
    }
}

class AppUpdates private constructor(context: Context) {
    private val context = context.applicationContext
    private val prefs = context.applicationContext.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val observers = CopyOnWriteArraySet<() -> Unit>()
    @Volatile var working = false; private set
    @Volatile var message = ""; private set
    @Volatile var release: AppRelease? = runCatching { AppRelease.parse(prefs.getString("release", "").orEmpty()) }.getOrNull(); private set
    @Volatile var downloaded = false; private set
    private var polling = false
    val feed: String get() = prefs.getString("feed", null)?.takeIf { it.isNotBlank() } ?: UpdateChannel.DEFAULT_FEED_URL
    val autoCheck: Boolean get() = prefs.getBoolean("auto", true)
    val available: AppRelease? get() = release?.takeIf { it.code > BuildConfig.VERSION_CODE && it.minSdk <= Build.VERSION.SDK_INT }
    val downloadId: Long get() = prefs.getLong("download_id", -1)

    init {
        if (release?.code?.let { it <= BuildConfig.VERSION_CODE } == true) {
            downloadId.takeIf { it != -1L }?.let { manager.remove(it) }
            prefs.edit().remove("download_id").apply()
            File(context.filesDir, "updates").deleteRecursively()
        }
    }
    fun configure(url: String, automatic: Boolean) {
        kotlin.check(!working) { "正在处理更新，请稍后保存" }
        val address = url.trim()
        require(address.isBlank() || AppRelease.validHttps(address)) { "更新源需要填写完整 HTTPS 地址" }
        if (feed != address) {
            downloadId.takeIf { it != -1L }?.let { manager.remove(it) }
            kotlin.check(prefs.edit().putString("feed", address).remove("release").remove("download_id").remove("last_check").commit())
            release = null; downloaded = false; message = ""
        }
        kotlin.check(prefs.edit().putBoolean("auto", automatic).commit())
        changed()
    }
    fun observe(observer: () -> Unit) { observers.add(observer); observer(); refreshDownload() }
    fun remove(observer: () -> Unit) { observers.remove(observer) }
    private fun changed() { main.post { observers.forEach { it() } } }
    private fun cacheFile(r: AppRelease): File = File(File(context.filesDir, "updates"), "rumo-${r.code}-${r.sha256}.apk")
    fun installerFile(): File? = available?.let { cacheFile(it).takeIf { file -> downloaded && file.isFile } }

    @Synchronized fun check(manual: Boolean = false) {
        if (working || downloadId != -1L) { if (downloadId != -1L) refreshDownload(); return }
        val source = feed
        if (source.isBlank()) { if (manual) { message = "请先配置更新源地址"; changed() }; return }
        val elapsed = System.currentTimeMillis() - prefs.getLong("last_check", 0)
        if (!manual && (!autoCheck || elapsed in 0 until 6 * 60 * 60 * 1000L)) return
        working = true; message = "正在检查更新…"; changed()
        worker.execute {
            try {
                val candidate = AppRelease.parse(readManifest(source))
                if (feed != source) return@execute
                release = candidate
                prefs.edit().putString("release", candidate.json()).putLong("last_check", System.currentTimeMillis()).apply()
                downloaded = available?.let { r -> cacheFile(r).isFile && runCatching { verify(cacheFile(r), r); true }.getOrDefault(false) } ?: false
                message = when {
                    candidate.code <= BuildConfig.VERSION_CODE -> "当前已是最新版本"
                    candidate.minSdk > Build.VERSION.SDK_INT -> "新版需要更高版本的 Android"
                    downloaded -> "${candidate.version} 已下载，点击安装即可"
                    else -> "发现新版本 ${candidate.version}"
                }
            } catch (_: Exception) { message = "检查失败，请确认更新源可访问后重试" }
            finally { working = false; changed() }
        }
    }

    @Synchronized fun download() {
        if (working) return
        if (downloadId != -1L) { refreshDownload(); return }
        val r = available ?: return
        if (downloaded && cacheFile(r).isFile) { changed(); return }
        working = true; message = "正在准备下载…"; changed()
        worker.execute {
            try {
                val cached = cacheFile(r)
                if (cached.isFile && runCatching { verify(cached, r); true }.getOrDefault(false)) {
                    downloaded = true; message = "已下载，无需重复下载，点击安装即可"
                } else {
                    cached.delete()
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: error("Storage unavailable")
                    val name = "rumo-${r.code}-${r.sha256}.apk"
                    File(dir, name).delete()
                    val request = DownloadManager.Request(android.net.Uri.parse(r.url))
                        .setTitle("入墨 ${r.version}").setDescription("下载完成后，返回入墨确认安装")
                        .setMimeType("application/vnd.android.package-archive")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, name)
                    val id = manager.enqueue(request)
                    kotlin.check(prefs.edit().putLong("download_id", id).commit())
                    message = "正在下载…"
                }
            } catch (_: Exception) { message = "无法开始下载，请稍后重试" }
            finally { working = false; changed(); main.post { poll() } }
        }
    }

    @Synchronized fun refreshDownload() {
        if (working) return
        val r = available ?: return
        val id = downloadId
        if (id == -1L && !cacheFile(r).isFile) return
        working = true
        worker.execute {
            try {
                if (id == -1L) {
                    verify(cacheFile(r), r); downloaded = true; message = "${r.version} 已下载，点击安装即可"
                } else manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                    if (!cursor.moveToFirst()) error("Download missing")
                    when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val uri = manager.getUriForDownloadedFile(id) ?: error("Missing file")
                            val output = cacheFile(r).also { it.parentFile?.mkdirs() }
                            val temp = File(output.path + ".part")
                            try {
                                context.contentResolver.openInputStream(uri)!!.use { input ->
                                    temp.outputStream().use { stream ->
                                        val buffer = ByteArray(32768); var total = 0L
                                        while (true) {
                                            val size = input.read(buffer); if (size == -1) break
                                            total += size; kotlin.check(total <= MAX_APK)
                                            stream.write(buffer, 0, size)
                                        }
                                    }
                                }
                                verify(temp, r)
                                kotlin.check(temp.renameTo(output))
                                output.parentFile?.listFiles()?.filter { it != output }?.forEach { it.delete() }
                            } finally { temp.delete() }
                            downloaded = true; message = "${r.version} 已下载，点击安装即可"
                            manager.remove(id); prefs.edit().remove("download_id").apply()
                        }
                        DownloadManager.STATUS_FAILED -> error("Download failed")
                        else -> {
                            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            if (bytes > MAX_APK || total > MAX_APK) error("Download too large")
                            message = if (total > 0) "正在下载 ${bytes * 100 / total}%" else "正在下载 / 等待网络…"
                        }
                    }
                }
            } catch (_: Exception) {
                if (id != -1L) { manager.remove(id); prefs.edit().remove("download_id").apply() }
                cacheFile(r).delete(); downloaded = false
                message = "下载或校验失败，请重试；本地草稿不受影响"
            } finally { working = false; changed(); main.post { poll() } }
        }
    }
    private fun poll() {
        if (polling || observers.isEmpty() || downloadId == -1L) return
        polling = true
        main.postDelayed({ polling = false; if (observers.isNotEmpty()) refreshDownload() }, 1200)
    }

    @Suppress("DEPRECATION")
    private fun verify(file: File, r: AppRelease) {
        require(file.length() in 1..MAX_APK)
        require(sha256(file) == r.sha256) { "Hash mismatch" }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = context.packageManager.getPackageArchiveInfo(file.path, flags) ?: error("Invalid APK")
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        require(candidate.packageName == context.packageName)
        val code = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        require(code == r.code.toLong() && code > BuildConfig.VERSION_CODE)
        fun certificates(info: android.content.pm.PackageInfo): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return signatures.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }.toSet()
        }
        val trusted = certificates(installed)
        require(trusted.isNotEmpty() && certificates(candidate) == trusted) { "Signing certificate mismatch" }
    }
    private fun readManifest(address: String): String {
        var url = address
        repeat(4) {
            require(AppRelease.validHttps(url))
            val connection = java.net.URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 12000; connection.readTimeout = 12000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Cache-Control", "no-cache")
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    url = URI(url).resolve(connection.getHeaderField("Location") ?: error("No redirect")).toString()
                } else {
                    kotlin.check(code == 200)
                    val bytes = connection.inputStream.use { it.readBytesLimited(65536) }
                    return String(bytes, Charsets.UTF_8)
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }
    companion object {
        private const val MAX_APK = 50 * 1024 * 1024L
        @Volatile private var instance: AppUpdates? = null
        fun get(context: Context): AppUpdates = instance ?: synchronized(this) { instance ?: AppUpdates(context).also { instance = it } }
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input -> val buffer = ByteArray(32768); while (true) { val n = input.read(buffer); if (n == -1) break; digest.update(buffer, 0, n) } }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (true) { val size = read(buffer); if (size == -1) break; check(output.size() + size <= limit); output.write(buffer, 0, size) }
    return output.toByteArray()
}
