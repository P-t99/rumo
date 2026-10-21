package com.hirden.flomofloat

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Used by both the main activity and the floating settings page. */
class SettingsContent(context: Context, private val store: LocalStore, private val installUpdate: () -> Unit) : LinearLayout(context) {
    private val updates = AppUpdates.get(context)
    private val updateStatus = Ui.text(context, "", 12f, Ui.muted)
    private val releaseNotes = Ui.text(context, "", 13f, Ui.muted)
    private val updateAction = Ui.button(context, "检查更新") {
        when {
            updates.downloaded -> installUpdate()
            updates.available != null -> updates.download()
            else -> updates.check(true)
        }
    }
    private val updateObserver: () -> Unit = { refreshUpdate() }
    private var appearanceRefresh: (() -> Unit)? = null
    private val appearanceObserver = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "overlay_mode" || key == "overlay_transparency") appearanceRefresh?.invoke()
    }
    init {
        orientation = VERTICAL
        addAppearance()
        val layoutCard = card("展开窗口布局")
        Ui.add(layoutCard, Ui.text(context, "拖动顶部圆点移动，拖动右下角缩放。尺寸与位置自动保存在本机，笔记 / 设置及横竖屏分别记忆。输入法出现时临时避让，收起后恢复。", 12f, Ui.muted), 6)
        Ui.add(layoutCard, Ui.button(context, "重置窗口布局") {
            runCatching { store.resetPanelLayouts() }.onSuccess { toast("窗口大小和位置已重置，草稿、API 和标签保留") }
                .onFailure { toast("重置失败，请重试") }
        }, 10)
        val apiCard = card("连接 flomo")
        Ui.add(apiCard, Ui.text(context, "粘贴 flomo 设置 → API 中的完整地址。", 12f, Ui.muted), 6)
        val api = Ui.edit(context, "https://flomoapp.com/iwh/…").apply {
            id = R.id.api_input; contentDescription = "flomo API 地址"; isSaveEnabled = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            transformationMethod = PasswordTransformationMethod.getInstance()
            setSingleLine(true); setText(store.api())
        }
        Ui.add(apiCard, api, 12)
        val apiMessage = Ui.text(context, when { store.apiReadFailed -> "读取失败，请重新保存 API"; api.text.isNotBlank() -> "已加密保存在本机"; else -> "尚未填写 API" }, 12f, Ui.muted)
        val reveal = CheckBox(context).apply {
            text = "显示地址"; textSize = 12f; setTextColor(Ui.muted)
            setOnCheckedChangeListener { _, checked -> api.transformationMethod = if (checked) null else PasswordTransformationMethod.getInstance(); api.setSelection(api.length()) }
        }
        Ui.add(apiCard, reveal)
        Ui.add(apiCard, Ui.button(context, "保存 API", true) {
            val value = api.text.toString().trim()
            val error = EndpointPolicy.error(value)
            if (error != null) { apiMessage.text = error; apiMessage.setTextColor(Ui.red) }
            else runCatching { store.saveApi(value) }.onSuccess {
                apiMessage.text = "已加密保存在本机"; apiMessage.setTextColor(Ui.green); toast("API 已保存")
            }.onFailure { apiMessage.text = "保存失败，请重试"; apiMessage.setTextColor(Ui.red) }
        })
        Ui.add(apiCard, apiMessage, 7)
        Ui.add(apiCard, Ui.button(context, "清除 API") {
            // Floating settings cannot show Activity dialogs; use a second explicit tap instead.
            if (api.tag == "confirm_delete") {
                runCatching { store.forgetApi() }.onSuccess { api.setText(""); api.tag = null; apiMessage.text = "API 已清除，草稿保留" }.onFailure { toast("清除失败，请重试") }
            } else {
                api.tag = "confirm_delete"; apiMessage.text = "再次点击“清除 API”确认，草稿会保留"
                api.postDelayed({ api.tag = null }, 5000)
            }
        }, 8)

        val tagCard = card("标签偏好")
        Ui.add(tagCard, Ui.text(context, "三个快捷标签可以修改，点击即可添加或移除。", 12f, Ui.muted), 6)
        val ids = listOf(R.id.preset_one, R.id.preset_two, R.id.preset_three)
        val fields = store.presets().mapIndexed { i, value ->
            Ui.edit(context, "快捷标签 ${i + 1}").apply {
                id = ids[i]; contentDescription = "快捷标签 ${i + 1}"; setSingleLine(true); setText(value)
                filters = arrayOf(android.text.InputFilter.LengthFilter(25))
            }.also { Ui.add(tagCard, it, 8) }
        }
        Ui.add(tagCard, Ui.text(context, "默认标签", 13f, bold = true), 14)
        val defaults = Ui.edit(context, "例如 #碎片 #灵感，可留空").apply {
            id = R.id.default_tags; contentDescription = "默认标签"; setSingleLine(true); setText(store.defaultTags())
        }
        Ui.add(tagCard, defaults, 7)
        Ui.add(tagCard, Ui.text(context, "每条新笔记自动填入。临时修改标签只影响当前笔记。", 12f, Ui.muted), 7)
        val tagMessage = Ui.text(context, "", 12f, Ui.muted)
        Ui.add(tagCard, Ui.button(context, "保存标签", true) {
            val values = fields.map { NoteProtocol.tags(it.text.toString()).removePrefix("#") }
            if (values.any { it.isBlank() || it.contains(" ") || it.length > 24 } || values.distinct().size != 3) {
                tagMessage.text = "请填写三个不同的标签，每个不超过 24 字且不含空格"; tagMessage.setTextColor(Ui.red)
            } else runCatching { store.saveTags(values, defaults.text.toString()) }.onSuccess {
                defaults.setText(store.defaultTags()); tagMessage.text = "已保存，新笔记自动使用默认标签"; tagMessage.setTextColor(Ui.green); toast("标签已保存")
            }.onFailure { tagMessage.text = "保存失败，请重试"; tagMessage.setTextColor(Ui.red) }
        }, 10)
        Ui.add(tagCard, tagMessage, 6)

        val updateCard = card("版本与更新")
        Ui.add(updateCard, Ui.text(context, "入墨 ${BuildConfig.VERSION_NAME} · 测试版", 14f, bold = true), 5)
        val feed = Ui.edit(context, "https://…/latest.json").apply {
            id = R.id.update_feed; contentDescription = "更新源地址"; setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(updates.feed)
        }
        Ui.add(updateCard, Ui.text(context, "更新源地址（已预置入墨通道，留空可恢复）", 12f, Ui.muted), 12)
        Ui.add(updateCard, feed, 7)
        val automatic = CheckBox(context).apply {
            text = "自动检查更新"; textSize = 14f; setTextColor(Ui.ink); isChecked = updates.autoCheck
        }
        Ui.add(updateCard, automatic, 2)
        Ui.add(updateCard, Ui.button(context, "保存更新设置") {
            runCatching { updates.configure(feed.text.toString(), automatic.isChecked) }.onSuccess {
                toast("更新设置已保存"); updates.check(true)
            }.onFailure { toast(it.message ?: "保存失败，请重试") }
        })
        Ui.add(updateCard, updateStatus, 10)
        Ui.add(updateCard, releaseNotes, 7)
        Ui.add(updateCard, updateAction, 10)
        Ui.add(updateCard, Ui.button(context, "重新检查") { updates.check(true) }, 7)
        Ui.add(updateCard, Ui.text(context, "使用期间每 6 小时检查一次。已下载的安装包会复用；安装时需要系统确认。", 12f, Ui.muted), 9)
        Ui.add(this, Ui.text(context, "基于 FloatingX 3.0.0 · 非 flomo 官方客户端\nAPI 加密保存，草稿仅存本机。", 11f, Ui.muted), 18)
        refreshUpdate()
    }
    private fun addAppearance() {
        val panel = card("悬浮入口")
        val initial = store.appearance()
        val modes = android.widget.RadioGroup(context).apply { orientation = HORIZONTAL; isSaveEnabled = false }
        for ((id, label) in listOf(R.id.mode_edge to "贴边竖条", R.id.mode_ball to "悬浮球")) {
            modes.addView(android.widget.RadioButton(context).apply {
                this.id = id; text = label; textSize = 14f; setTextColor(Ui.ink); isSaveEnabled = false
                minHeight = Ui.dp(context, 48)
            }, LayoutParams(0, Ui.dp(context, 48), 1f))
        }
        modes.check(if (initial.mode == OverlayMode.EDGE) R.id.mode_edge else R.id.mode_ball)
        Ui.add(panel, modes, 5)
        val caption = Ui.text(context, "透明度：${initial.transparency}%", 13f)
        Ui.add(panel, caption, 9)
        val seek = android.widget.SeekBar(context).apply {
            id = R.id.floating_transparency; contentDescription = "悬浮入口透明度"; isSaveEnabled = false
            max = 80; progress = initial.transparency
        }
        Ui.add(panel, seek, 4, Ui.dp(context, 48))
        val preview = android.widget.FrameLayout(context).apply { background = Ui.shape(context, Ui.bg, 10) }
        Ui.add(panel, preview, 5, Ui.dp(context, 72))
        var rendering = false
        fun render() {
            if (rendering) return
            rendering = true
            try {
                val value = store.appearance()
                modes.check(if (value.mode == OverlayMode.BALL) R.id.mode_ball else R.id.mode_edge)
                seek.progress = value.transparency
                caption.text = "透明度：${value.transparency}%"
                preview.removeAllViews()
                val entry = value.createEntry(context)
                preview.addView(entry, android.widget.FrameLayout.LayoutParams(entry.layoutParams.width, entry.layoutParams.height, android.view.Gravity.CENTER))
            } finally { rendering = false }
        }
        fun applySelection() {
            if (rendering) return
            val mode = if (modes.checkedRadioButtonId == R.id.mode_ball) OverlayMode.BALL else OverlayMode.EDGE
            store.saveAppearance(FloatingAppearance(mode, seek.progress))
            render()
        }
        appearanceRefresh = { render() }
        modes.setOnCheckedChangeListener { _, _ -> applySelection() }
        seek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) { applySelection() }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        render()
        Ui.add(panel, Ui.text(context, "即时生效并自动保存。0% 最清晰，80% 更淡；编辑笔记时保持清晰。", 12f, Ui.muted), 8)
    }
    private fun card(title: String): LinearLayout = Ui.column(context, 16).also {
        it.background = Ui.shape(context, Color.WHITE, 12, Ui.line)
        Ui.add(this, it, if (childCount == 0) 0 else 12)
        Ui.add(it, Ui.text(context, title, 16f, bold = true))
    }
    private fun refreshUpdate() {
        updateStatus.text = if (updates.feed.isBlank()) "尚未配置更新源" else updates.message.ifBlank { "可手动检查更新" }
        releaseNotes.text = updates.available?.notes.orEmpty()
        releaseNotes.visibility = if (releaseNotes.text.isBlank()) View.GONE else View.VISIBLE
        updateAction.text = when {
            updates.downloaded -> "安装更新"
            updates.downloadId != -1L -> "下载中…"
            updates.available != null -> "下载 ${updates.available?.version}"
            else -> "检查更新"
        }
        updateAction.isEnabled = !updates.working && updates.downloadId == -1L && updates.feed.isNotBlank()
        updateAction.alpha = if (updateAction.isEnabled) 1f else .5f
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); updates.observe(updateObserver); store.observeAppearance(appearanceObserver); appearanceRefresh?.invoke() }
    override fun onDetachedFromWindow() { updates.remove(updateObserver); store.removeAppearanceObserver(appearanceObserver); super.onDetachedFromWindow() }
    private fun toast(value: String) { Toast.makeText(context, value, Toast.LENGTH_LONG).show() }
}
