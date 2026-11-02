package com.hirden.flomofloat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.ViewGroup

/** A curved edge handle and its in-place close button share one fixed touch area. */
class EdgeTabView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val location = IntArray(2)
    private var foreground = 0
    private var primary = 0
    var expanded: Boolean = false
        set(value) {
            field = value
            contentDescription = if (value) "收起入墨笔记，草稿自动保存" else "入墨弧形悬浮条，点击写笔记，拖动调整位置"
            invalidate()
        }
    init {
        Ui.bind(this, "paint") { primary = it[Ui.primary]; foreground = it[Ui.onPrimary]; invalidate() }
        expanded = false
        layoutParams = ViewGroup.LayoutParams(Ui.dp(context, 40), Ui.dp(context, 72))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = primary
        if (expanded) {
            val cx = width / 2f; val cy = height / 2f
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, 16f * density, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * density; paint.color = foreground
            val arm = 5f * density
            canvas.drawLine(cx - arm, cy - arm, cx + arm, cy + arm, paint)
            canvas.drawLine(cx + arm, cy - arm, cx - arm, cy + arm, paint)
            return
        }
        getLocationOnScreen(location)
        val leftEdge = location[0] + width / 2 < resources.displayMetrics.widthPixels / 2
        val edge = if (leftEdge) 3f * density else width - 3f * density
        val inward = if (leftEdge) 10f * density else -10f * density
        path.reset()
        path.moveTo(edge, 10f * density)
        path.cubicTo(edge + inward, height * .32f, edge + inward, height * .68f, edge, height - 10f * density)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f * density
        canvas.drawPath(path, paint)
    }
}
