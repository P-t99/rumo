package com.hirden.flomofloat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxHalfHide
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.storage.FxSpStorage
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import com.petterp.floatingx.system.systemHost
import com.petterp.floatingx.system.container.FxWindowContainer

class CaptureService : Service() {
    private lateinit var store: LocalStore
    private var bubble: FxControl? = null
    private var appliedAppearance: FloatingAppearance? = null
    private val appearanceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "overlay_mode" || key == "overlay_transparency") applyAppearance()
        if (key == "layout_reset_revision") panelLayout?.reset()
        if (key == AppTheme.PREFERENCE) refreshTheme()
    }
    private var editor: FxControl? = null
    private var panelLayout: PanelWindowController? = null
    private var panelInput: OverlayInputController? = null
    private var input: EditText? = null
    private var tags: EditText? = null
    private var status: TextView? = null
    private var count: TextView? = null
    private var sendButton: Button? = null
    private val chipButtons = mutableListOf<Pair<Button, String>>()
    private var restoring = false
    private var settingsPage = false
    private var lastConfiguration: Configuration? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateTick = object : Runnable {
        override fun run() { AppUpdates.get(this@CaptureService).check(); handler.postDelayed(this, 6 * 60 * 60 * 1000L) }
    }
    private val sendListener: (SendResult?) -> Unit = { result ->
        updateBusy()
        if (result != null) {
            if (result.success && store.draft().text.isBlank()) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                if (!settingsPage) collapse(persistDraft = false)
            } else { status?.text = result.message; status?.let { Ui.textColor(it, if (result.success) Ui.green else Ui.red) } }
        } else { status?.text = "正在存入 flomo…"; status?.let { Ui.textColor(it, Ui.green) } }
    }
    override fun onCreate() {
        super.onCreate(); setTheme(AppTheme.style(this)); lastConfiguration = Configuration(resources.configuration)
        store = LocalStore(this); store.observeAppearance(appearanceListener); SendManager.observe(sendListener); handler.post(updateTick)
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        try {
            startAsForeground(); running = true
            if (bubble == null) installBubble()
            if (intent?.action == EXPAND) expand() else if (editor == null) bubble?.show()
        } catch (_: Exception) {
            Toast.makeText(this, "无法显示悬浮窗，请重新开启悬浮权限后再试", Toast.LENGTH_LONG).show(); stopSelf()
        }
        return START_NOT_STICKY
    }
    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "悬浮速记", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 2, Intent(this, CaptureService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("入墨 · 悬浮速记已开启").setContentText("点击悬浮入口，即可写下想法")
            .setContentIntent(open).setOngoing(true).setShowWhen(false)
            .addAction(Notification.Action.Builder(null, "停止悬浮", stop).build()).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(32, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(32, notification)
    }
    private fun installBubble(show: Boolean = true) {
        val appearance = store.appearance()
        val entry = appearance.createEntry(this)
        appliedAppearance = appearance
        bubble = FloatingX.install(BUBBLE) {
            view(entry); anchor(FxGravity.CENTER_END)
            margin(top = Ui.dp(this@CaptureService, 48).toFloat(), bottom = Ui.dp(this@CaptureService, 48).toFloat())
            adsorb(FxAdsorb.horizontal(if (appearance.mode == OverlayMode.BALL) FxHalfHide(.08f) else null))
            persist(FxSpStorage(applicationContext, if (appearance.mode == OverlayMode.EDGE) "rumo_tab_position" else "rumo_bubble_position"))
            systemHost(applicationContext) { permission(FxPermissionStrategy.skip()) }
        }.also {
            it.addListener(object : FxListener {
                override fun onClick(control: FxControl, view: View) { expand() }
                override fun onPositionChanged(control: FxControl, anchor: FxAnchor) { entry.invalidate() }
                override fun onDrag(control: FxControl, x: Float, y: Float) { entry.invalidate() }
            })
            if (show) it.show()
        }
    }
    private fun applyAppearance() {
        val current = bubble ?: return
        val appearance = store.appearance()
        if (appearance == appliedAppearance) return
        if (appliedAppearance?.mode == appearance.mode) {
            current.contentView?.alpha = appearance.opacity
            appliedAppearance = appearance
        } else {
            val visible = current.isShowing && editor == null
            current.cancel(); bubble = null
            installBubble(show = visible)
        }
    }
    private fun shell(isSettings: Boolean): SwipeLayout {
        return SwipeLayout(this) { collapse() }.apply {
            setPadding(Ui.dp(this@CaptureService, 12), Ui.dp(this@CaptureService, 6), Ui.dp(this@CaptureService, 12), Ui.dp(this@CaptureService, 6))
            Ui.background(this, if (isSettings) Ui.bg else Ui.surface, 16, Ui.line)
            elevation = Ui.dp(this@CaptureService, 12).toFloat()
            layoutParams = ViewGroup.LayoutParams(Ui.dp(this@CaptureService, 360), Ui.dp(this@CaptureService, 360))
        }
    }
    private fun addPanelHeader(shell: SwipeLayout, title: String) {
        val header = Ui.row(this)
        header.addView(panelHandle(resize = false), LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)))
        header.addView(Ui.text(this, title, 18f, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(Ui.button(this, "重置") {
            runCatching { store.resetPanelLayouts() }.onSuccess {
                Toast.makeText(this, "窗口布局已重置，草稿保留", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(this, "重置失败，请重试", Toast.LENGTH_SHORT).show() }
        }.apply { id = R.id.reset_panel_layout; textSize = 12f; minWidth = 0; minimumWidth = 0; setPadding(0, 0, 0, 0); contentDescription = "重置窗口大小和位置" },
            LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 40)))
        Ui.add(shell, header, height = Ui.dp(this, 40))
    }
    private fun panelHandle(resize: Boolean) = PanelHandleView(this, resize,
        begin = { panelLayout?.begin(resize) }, change = { x, y -> panelLayout?.change(x, y) }, finish = { panelLayout?.finish(it) }
    ).apply { id = if (resize) R.id.resize_handle else R.id.drag_handle }
    private fun expand() {
        if (editor != null) return
        settingsPage = false; bubble?.hide(); restoring = true
        val shell = shell(false)
        addPanelHeader(shell, "随手记")
        val bodyScroll = ScrollView(this).apply { isFillViewport = true }
        val body = Ui.column(this); bodyScroll.addView(body)
        shell.addView(bodyScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val draft = store.draft()
        input = Ui.edit(this, "此刻，有什么想法？").apply {
            id = R.id.note_input; contentDescription = "笔记正文"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            gravity = Gravity.TOP or Gravity.START; minLines = 2; maxLines = 8; textSize = 17f
            Ui.clearBackground(this); setPadding(0, Ui.dp(this@CaptureService, 12), 0, Ui.dp(this@CaptureService, 8))
            setText(draft.text); setSelection(length())
        }
        Ui.add(body, input!!, 4)
        tags = Ui.edit(this, "#标签").apply {
            id = R.id.note_tags; contentDescription = "笔记标签"
            inputType = InputType.TYPE_CLASS_TEXT; setSingleLine(true); setText(draft.tags); textSize = 13f
            Ui.textColor(this, Ui.green); Ui.clearBackground(this); setPadding(0, Ui.dp(this@CaptureService, 8), 0, Ui.dp(this@CaptureService, 8))
        }
        Ui.add(body, tags!!, 2)
        val chips = Ui.row(this); chipButtons.clear()
        for (tag in store.presets()) {
            val button = Ui.button(this, "#$tag") {
                val current = NoteProtocol.tags(tags?.text.toString()).split(" ").filter { it.isNotBlank() }.toMutableList()
                if (!current.remove("#$tag")) current.add("#$tag")
                tags?.setText(current.joinToString(" ")); tags?.setSelection(tags?.length() ?: 0)
            }.apply { textSize = 12f; minWidth = 0; minimumWidth = 0; minHeight = Ui.dp(this@CaptureService, 36); minimumHeight = minHeight; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(Ui.dp(this@CaptureService, 7), 0, Ui.dp(this@CaptureService, 7), 0) }
            chips.addView(button, LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1f).apply { rightMargin = Ui.dp(this@CaptureService, 5) })
            chipButtons.add(button to "#$tag")
        }
        Ui.add(body, chips, 4)
        count = Ui.text(this, "${draft.text.length} 字 · 草稿自动保存", 11f, Ui.muted)
        Ui.add(body, count!!, 9)
        status = Ui.text(this, if (SendManager.busy) "正在存入 flomo…" else "直接输入 · 空白处轻左滑或双击收起", 11f, Ui.muted)
        Ui.add(body, status!!, 5)
        val footer = Ui.row(this)
        footer.addView(Ui.button(this, "设置") { showSettings() }.apply { textSize = 13f; setPadding(Ui.dp(this@CaptureService, 9), 0, Ui.dp(this@CaptureService, 9), 0); minimumWidth = 0; minWidth = 0 }, LinearLayout.LayoutParams(-2, Ui.dp(this, 46)))
        footer.addView(Ui.button(this, "收起") { collapse() }.apply { textSize = 13f; setPadding(Ui.dp(this@CaptureService, 9), 0, Ui.dp(this@CaptureService, 9), 0); minimumWidth = 0; minWidth = 0 }, LinearLayout.LayoutParams(-2, Ui.dp(this, 46)).apply { leftMargin = Ui.dp(this@CaptureService, 5) })
        sendButton = Ui.button(this, "存入 flomo", true) {
            if (!saveDraft()) return@button
            SendManager.send(this)?.let { status?.text = it; status?.let { Ui.textColor(it, Ui.red) } }
        }.apply { textSize = 14f; minWidth = 0; minimumWidth = 0 }
        footer.addView(sendButton, LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f).apply { leftMargin = Ui.dp(this@CaptureService, 6) })
        footer.addView(panelHandle(resize = true), LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 46)))
        Ui.add(shell, footer, 6)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { if (!restoring) { saveDraft(); refreshChips() } }
            override fun afterTextChanged(s: Editable?) {}
        }
        input!!.addTextChangedListener(watcher); tags!!.addTextChangedListener(watcher)
        restoring = false; showPanel(shell); updateBusy(); refreshChips()
        panelInput?.focusEditor(R.id.note_input)
    }
    private fun showSettings() {
        if (!saveDraft()) return
        closePanel(); settingsPage = true
        val shell = shell(true)
        addPanelHeader(shell, "设置")
        val scroll = ScrollView(this)
        scroll.addView(SettingsContent(this, store) {
            // Launch while this overlay remains visible; only then collapse it.
            try {
                startActivity(Intent(this, MainActivity::class.java).setAction(MainActivity.INSTALL_UPDATE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                collapse()
            } catch (_: Exception) { Toast.makeText(this, "请从入墨主界面点击安装更新", Toast.LENGTH_LONG).show() }
        })
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = Ui.dp(this@CaptureService, 12) })
        val footer = Ui.row(this)
        footer.addView(Ui.button(this, "返回笔记", true) { closePanel(); expand() }, LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f))
        footer.addView(Ui.button(this, "收起") { collapse() }, LinearLayout.LayoutParams(-2, Ui.dp(this, 46)).apply { leftMargin = Ui.dp(this@CaptureService, 8) })
        footer.addView(panelHandle(resize = true), LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 46)))
        Ui.add(shell, footer, 6); showPanel(shell)
    }
    private fun showPanel(shell: View) {
        val layout = PanelWindowController(this, store, shell, settingsPage)
        panelLayout = layout
        editor = FloatingX.install(EDITOR) {
            view(shell); safeArea = false
            anchor(FxGravity.TOP_START, dx = layout.frame.x.toFloat(), dy = layout.frame.y.toFloat())
            gesture { click = false; longPress = false; drag = FxDrag.DISABLED }
            systemHost(applicationContext) {
                permission(FxPermissionStrategy.skip())
                layoutParams {
                    it.flags = (it.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()) or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    it.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                }
                onBackPressed { if (settingsPage) { closePanel(); expand() } else collapse(); true }
            }
        }.also {
            it.show(); layout.attach(it)
            panelInput = (shell.parent as? FxWindowContainer)?.let { container ->
                OverlayInputController(container, intArrayOf(R.id.note_input, R.id.note_tags, R.id.api_input,
                    R.id.preset_one, R.id.preset_two, R.id.preset_three, R.id.default_tags, R.id.update_feed))
            }
        }
    }
    private fun refreshChips() {
        val selected = NoteProtocol.tags(tags?.text.toString()).split(" ").toSet()
        chipButtons.forEach { (button, value) ->
            val checked = value in selected
            button.isSelected = checked
            button.contentDescription = "$value，${if (checked) "已选中" else "未选中"}"
            Ui.background(button, if (checked) Ui.pale else Ui.bg, 8, if (checked) Ui.green else Ui.line)
            Ui.textColor(button, if (checked) Ui.green else Ui.muted)
        }
    }
    private fun saveDraft(): Boolean {
        val field = input ?: return true
        if (SendManager.busy) return true
        return runCatching {
            store.saveDraft(Draft(field.text.toString(), tags?.text.toString())); count?.text = "${field.length()} 字 · 草稿已保存"; true
        }.getOrElse { status?.text = "本地草稿保存失败，请暂时保留此窗口"; status?.let { Ui.textColor(it, Ui.red) }; false }
    }
    private fun updateBusy() {
        input?.isEnabled = !SendManager.busy; tags?.isEnabled = !SendManager.busy
        chipButtons.forEach { it.first.isEnabled = !SendManager.busy }
        sendButton?.isEnabled = !SendManager.busy; sendButton?.text = if (SendManager.busy) "存入中…" else "存入 flomo"
        sendButton?.alpha = if (SendManager.busy) .6f else 1f
    }
    private fun closePanel() {
        panelInput?.close(); panelInput = null
        panelLayout?.close(); panelLayout = null
        editor?.cancel(); editor = null
        input = null; tags = null; status = null; count = null; sendButton = null; chipButtons.clear()
    }
    private fun collapse(persistDraft: Boolean = true) {
        if (persistDraft && !saveDraft()) return
        closePanel(); settingsPage = false; bubble?.show()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val changes = lastConfiguration?.diff(newConfig) ?: 0
        lastConfiguration = Configuration(newConfig)
        refreshTheme()
        val geometry = ActivityInfo.CONFIG_ORIENTATION or ActivityInfo.CONFIG_SCREEN_SIZE or
            ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or ActivityInfo.CONFIG_SCREEN_LAYOUT or
            ActivityInfo.CONFIG_DENSITY or ActivityInfo.CONFIG_LAYOUT_DIRECTION or ActivityInfo.CONFIG_FONT_SCALE
        if (editor != null && changes and geometry != 0) {
            val wasSettings = settingsPage
            if (!saveDraft()) return
            closePanel(); if (wasSettings) showSettings() else expand()
        }
    }
    private fun refreshTheme() {
        setTheme(AppTheme.style(this))
        editor?.contentView?.let(Ui::refresh)
        bubble?.contentView?.let(Ui::refresh)
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); store.removeAppearanceObserver(appearanceListener); SendManager.remove(sendListener)
        runCatching { closePanel() }; runCatching { bubble?.cancel() }; bubble = null; running = false
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy()
    }
    companion object {
        const val START = "com.hirden.flomofloat.START"; const val EXPAND = "com.hirden.flomofloat.EXPAND"; const val STOP = "com.hirden.flomofloat.STOP"
        private const val CHANNEL = "rumo_capture"; private const val BUBBLE = "rumo-tab"; private const val EDITOR = "rumo-editor"
        var running = false; private set
    }
}
