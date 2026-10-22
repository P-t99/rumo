package com.hirden.flomofloat

import android.content.Context
import android.content.res.ColorStateList
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
    val bg = Tone.BG
    val surface = Tone.SURFACE
    val field = Tone.FIELD
    val ink = Tone.INK
    val muted = Tone.MUTED
    val green = Tone.GREEN
    val primary = Tone.PRIMARY
    val onPrimary = Tone.ON_PRIMARY
    val pale = Tone.PALE
    val line = Tone.LINE
    val red = Tone.RED

    @Suppress("UNCHECKED_CAST")
    fun bind(view: View, key: String, render: (ThemePalette) -> Unit) {
        val bindings = (view.getTag(R.id.theme_bindings) as? MutableMap<String, (ThemePalette) -> Unit>)
            ?: linkedMapOf<String, (ThemePalette) -> Unit>().also { view.setTag(R.id.theme_bindings, it) }
        bindings[key] = render
        if (android.os.Build.VERSION.SDK_INT >= 29) view.isForceDarkAllowed = false
        render(AppTheme.palette(view.context))
    }
    fun refresh(root: View) {
        val palette = AppTheme.palette(root.context)
        fun visit(view: View) {
            @Suppress("UNCHECKED_CAST")
            (view.getTag(R.id.theme_bindings) as? Map<String, (ThemePalette) -> Unit>)?.values?.toList()?.forEach { it(palette) }
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        visit(root)
    }
    fun textColor(view: TextView, tone: Tone) = bind(view, "text") { view.setTextColor(it[tone]) }
    fun background(view: View, fill: Tone, radius: Int = 18, stroke: Tone? = null) = bind(view, "background") {
        view.background = shape(view.context, it[fill], radius, stroke?.let(it::get))
    }
    fun clearBackground(view: View) {
        @Suppress("UNCHECKED_CAST")
        (view.getTag(R.id.theme_bindings) as? MutableMap<String, (ThemePalette) -> Unit>)?.remove("background")
        view.background = null
    }
    fun control(view: android.widget.CompoundButton, text: Tone = ink) {
        textColor(view, text)
        bind(view, "control") { palette ->
            view.buttonTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(palette[green], palette[muted]))
        }
    }
    fun seek(view: android.widget.SeekBar) = bind(view, "seek") {
        view.progressTintList = ColorStateList.valueOf(it[green])
        view.thumbTintList = ColorStateList.valueOf(it[green])
        view.progressBackgroundTintList = ColorStateList.valueOf(it[line])
    }
    fun dp(c: Context, value: Int) = (value * c.resources.displayMetrics.density).toInt()
    fun shape(c: Context, color: Int, radius: Int = 18, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(c, radius).toFloat()
        if (stroke != null) setStroke(dp(c, 1), stroke)
    }
    fun text(c: Context, value: String, size: Float = 15f, color: Tone = ink, bold: Boolean = false): TextView = TextView(c).apply {
        text = value; textSize = size; textColor(this, color)
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
        textColor(this, if (primary) onPrimary else green)
        bind(this, "background") { palette ->
            background = RippleDrawable(ColorStateList.valueOf(if (palette.dark) 0x2278d3aa else 0x22336644),
                shape(c, palette[if (primary) Ui.primary else pale], 10), null)
        }
        setPadding(dp(c, 14), dp(c, 6), dp(c, 14), dp(c, 6))
        setOnClickListener { action() }
    }
    fun edit(c: Context, hint: String): EditText = OverlayEditText(c).apply {
        this.hint = hint; textSize = 15f; textColor(this, ink)
        bind(this, "editor") { palette ->
            setHintTextColor(palette[muted]); highlightColor = (palette[green] and 0x00ffffff) or 0x44000000
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                textCursorDrawable = shape(c, palette[green], 1).apply { setSize(dp(c, 2), dp(c, 24)) }
                textSelectHandleLeft?.mutate()?.setTint(palette[green])
                textSelectHandleRight?.mutate()?.setTint(palette[green])
                textSelectHandle?.mutate()?.setTint(palette[green])
            }
        }
        imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
        Ui.background(this, field, 12, line)
        setPadding(dp(c, 13), dp(c, 12), dp(c, 13), dp(c, 12))
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
    fun add(parent: LinearLayout, child: View, top: Int = 0, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) {
        parent.addView(child, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply { topMargin = dp(parent.context, top) })
    }
    fun space(parent: LinearLayout, height: Int) { parent.addView(View(parent.context), LinearLayout.LayoutParams(1, dp(parent.context, height))) }
}
