package com.hirden.flomofloat

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup

enum class OverlayMode { EDGE, BALL }
data class FloatingAppearance(val mode: OverlayMode = OverlayMode.EDGE, val transparency: Int = 20) {
    val opacity: Float get() = 1f - transparency.coerceIn(0, 80) / 100f
    fun createEntry(context: Context): View = when (mode) {
        OverlayMode.EDGE -> EdgeTabView(context)
        OverlayMode.BALL -> Ui.text(context, "墨", 21f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            contentDescription = "入墨悬浮球，点击写笔记，拖动调整位置"
            background = Ui.shape(context, Ui.green, 24)
            elevation = Ui.dp(context, 4).toFloat()
            layoutParams = ViewGroup.LayoutParams(Ui.dp(context, 48), Ui.dp(context, 48))
        }
    }.apply { alpha = opacity }
}
