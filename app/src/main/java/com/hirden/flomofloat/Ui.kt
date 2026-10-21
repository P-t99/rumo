package com.hirden.flomofloat

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    val bg = Color.rgb(247, 248, 247)
    val ink = Color.rgb(48, 51, 49)
    val muted = Color.rgb(102, 117, 107)
    val green = Color.rgb(48, 158, 112)
    val pale = Color.rgb(236, 247, 240)
    val line = Color.rgb(233, 237, 234)
    val red = Color.rgb(164, 54, 46)
    fun dp(c: Context, value: Int) = (value * c.resources.displayMetrics.density).toInt()
    fun shape(c: Context, color: Int, radius: Int = 18, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(c, radius).toFloat()
        if (stroke != null) setStroke(dp(c, 1), stroke)
    }
    fun text(c: Context, value: String, size: Float = 15f, color: Int = ink, bold: Boolean = false): TextView = TextView(c).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(c, 3).toFloat(), 1f)
    }
    fun column(c: Context, pad: Int = 0) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        val p = dp(c, pad); setPadding(p, p, p, p)
    }
    fun row(c: Context) = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
    fun button(c: Context, label: String, primary: Boolean = false, action: () -> Unit): Button = Button(c).apply {
        text = label; textSize = 15f; isAllCaps = false; minHeight = dp(c, 48)
        setTextColor(if (primary) Color.WHITE else green)
        background = RippleDrawable(ColorStateList.valueOf(0x22336644), shape(c, if (primary) green else pale, 10), null)
        setPadding(dp(c, 14), dp(c, 6), dp(c, 14), dp(c, 6))
        setOnClickListener { action() }
    }
    fun edit(c: Context, hint: String): EditText = EditText(c).apply {
        this.hint = hint; textSize = 15f; setTextColor(ink); setHintTextColor(muted)
        imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
        background = shape(c, Color.rgb(248, 250, 247), 12, line)
        setPadding(dp(c, 13), dp(c, 12), dp(c, 13), dp(c, 12))
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
    fun add(parent: LinearLayout, child: View, top: Int = 0, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) {
        parent.addView(child, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply { topMargin = dp(parent.context, top) })
    }
    fun space(parent: LinearLayout, height: Int) { parent.addView(View(parent.context), LinearLayout.LayoutParams(1, dp(parent.context, height))) }
}
