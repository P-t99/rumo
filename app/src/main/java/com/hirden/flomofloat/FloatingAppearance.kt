package com.hirden.flomofloat

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup

enum class OverlayMode {
    EDGE, BALL, VERTICAL, HORIZONTAL;
    val hasCloseEntry: Boolean get() = this != BALL
    val label: String get() = when (this) {
        EDGE -> "弧形悬浮条"; BALL -> "悬浮球"; VERTICAL -> "直竖条"; HORIZONTAL -> "直横条"
    }
    val positionKey: String get() = when (this) {
        EDGE -> "rumo_tab_position"; BALL -> "rumo_bubble_position"
        VERTICAL -> "rumo_vertical_position"; HORIZONTAL -> "rumo_horizontal_position"
    }
}
data class FloatingAppearance(val mode: OverlayMode = OverlayMode.EDGE, val transparency: Int = 20) {
    val opacity: Float get() = 1f - transparency.coerceIn(0, 80) / 100f
    fun createEntry(context: Context): View = when (mode) {
        OverlayMode.EDGE, OverlayMode.VERTICAL, OverlayMode.HORIZONTAL -> EdgeTabView(context, mode)
        OverlayMode.BALL -> Ui.text(context, "墨", 21f, Ui.onPrimary, true).apply {
            gravity = Gravity.CENTER
            contentDescription = "入墨悬浮球，点击写笔记，拖动调整位置"
            Ui.background(this, Ui.primary, 24)
            elevation = Ui.dp(context, 4).toFloat()
            layoutParams = ViewGroup.LayoutParams(Ui.dp(context, 48), Ui.dp(context, 48))
        }
    }.apply { alpha = opacity }
}
