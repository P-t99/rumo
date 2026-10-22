package com.hirden.flomofloat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup

/** A narrow visible tab with a wider, transparent touch area for reliable tapping. */
class EdgeTabView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val location = IntArray(2)
    init {
        Ui.bind(this, "paint") { paint.color = it[Ui.green]; invalidate() }
        contentDescription = "入墨贴边竖条，点击写笔记，拖动调整位置"
        layoutParams = ViewGroup.LayoutParams(Ui.dp(context, 24), Ui.dp(context, 64))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    override fun onDraw(canvas: Canvas) {
        getLocationOnScreen(location)
        val tabWidth = Ui.dp(context, 6).toFloat()
        val left = if (location[0] < resources.displayMetrics.widthPixels / 2) 0f else width - tabWidth
        canvas.drawRoundRect(left, Ui.dp(context, 8).toFloat(), left + tabWidth, height - Ui.dp(context, 8).toFloat(), tabWidth / 2, tabWidth / 2, paint)
    }
}
