package com.hirden.flomofloat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider

class MainActivity : Activity() {
    private lateinit var store: LocalStore
    private lateinit var overlayStatus: TextView
    private lateinit var draftStatus: TextView
    private var waitingForOverlay = false
    private var pendingExpand = false
    private var waitingForInstall = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        waitingForOverlay = savedInstanceState?.getBoolean("waiting") ?: false
        pendingExpand = savedInstanceState?.getBoolean("expand") ?: false
        waitingForInstall = savedInstanceState?.getBoolean("install") ?: false
        window.statusBarColor = Ui.bg; window.navigationBarColor = Ui.bg
        val scroll = ScrollView(this).apply { setBackgroundColor(Ui.bg); isFillViewport = true }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        val page = Ui.column(this, 20)
        scroll.addView(page); setContentView(scroll)
        Ui.add(page, Ui.text(this, "入墨", 30f, bold = true), 14)
        Ui.add(page, Ui.text(this, "随时记录，回到 flomo。", 14f, Ui.muted), 7)
        val capture = Ui.column(this, 16).apply { background = Ui.shape(this@MainActivity, android.graphics.Color.WHITE, 12, Ui.line) }
        Ui.add(page, capture, 22)
        overlayStatus = Ui.text(this, "", 13f, Ui.muted)
        Ui.add(capture, overlayStatus)
        Ui.add(capture, Ui.button(this, "开始悬浮速记", true) { enableBubble() }, 14)
        val actions = Ui.row(this)
        actions.addView(Ui.button(this, "写一条") { enableBubble(true) }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(Ui.button(this, "停止悬浮") {
            stopService(Intent(this, CaptureService::class.java)); mainHandler.postDelayed({ refresh() }, 250)
        }, android.widget.LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = Ui.dp(this@MainActivity, 8) })
        Ui.add(capture, actions, 8)
        Ui.add(capture, Ui.text(this, "点击竖条或悬浮球写笔记，拖动可调整位置。展开后，轻左滑或双击标题、空白处即可收起。", 12f, Ui.muted), 12)
        draftStatus = Ui.text(this, "", 12f, Ui.muted)
        Ui.add(capture, draftStatus, 9)
        Ui.add(page, SettingsContent(this, store) { installUpdate() }, 16)
        Ui.add(page, Ui.text(this, "部分手机需要在系统设置中允许后台运行，才能持续显示悬浮入口。", 12f, Ui.muted), 14)
        if (intent.action == INSTALL_UPDATE) mainHandler.post { installUpdate() }
    }
    private fun enableBubble(expand: Boolean = false) {
        EndpointPolicy.error(store.api())?.let { toast(it); return }
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlay = true; pendingExpand = expand
            try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
            catch (_: Exception) { waitingForOverlay = false; toast("请在系统设置中为入墨开启悬浮窗权限") }
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !getPreferences(MODE_PRIVATE).getBoolean("notification_asked", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("notification_asked", true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
        try {
            startForegroundService(Intent(this, CaptureService::class.java).apply { action = if (expand) CaptureService.EXPAND else CaptureService.START })
            mainHandler.postDelayed({ refresh() }, 350)
        } catch (_: Exception) { toast("启动失败，请返回前台后重试") }
    }
    private fun installUpdate() {
        val file = AppUpdates.get(this).installerFile() ?: run { toast("请先完成下载和校验"); return }
        if (!packageManager.canRequestPackageInstalls()) {
            waitingForInstall = true
            try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
            catch (_: Exception) { waitingForInstall = false; toast("请在系统设置中允许入墨安装应用") }
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.updates", file)
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: Exception) { toast("无法打开安装界面，请稍后重试") }
    }
    private fun refresh() {
        overlayStatus.text = when {
            !Settings.canDrawOverlays(this) -> "首次使用，请先保存 API 并开启悬浮权限"
            CaptureService.running -> "● 悬浮速记运行中"
            else -> "悬浮权限已开启，可以开始记录"
        }
        overlayStatus.setTextColor(if (CaptureService.running) Ui.green else Ui.muted)
        val draft = store.draft()
        draftStatus.text = (if (draft.text.isBlank()) "草稿自动保存在本机" else "已保留 ${draft.text.length} 字未发送草稿") +
            store.lastStatus.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
    }
    override fun onResume() {
        super.onResume()
        if (!::store.isInitialized) return
        if (waitingForOverlay) {
            waitingForOverlay = false
            if (Settings.canDrawOverlays(this)) mainHandler.post { enableBubble(pendingExpand) }
        }
        if (waitingForInstall) {
            waitingForInstall = false
            if (packageManager.canRequestPackageInstalls()) mainHandler.post { installUpdate() }
        }
        AppUpdates.get(this).check()
        AppUpdates.get(this).refreshDownload()
        refresh()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("waiting", waitingForOverlay); outState.putBoolean("expand", pendingExpand); outState.putBoolean("install", waitingForInstall)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { mainHandler.removeCallbacksAndMessages(null); super.onDestroy() }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    companion object { const val INSTALL_UPDATE = "com.hirden.flomofloat.INSTALL_UPDATE" }
}
