package com.hirden.flomofloat

import android.content.Context
import android.content.res.Configuration

enum class ThemeMode { SYSTEM, LIGHT, DARK;
    companion object {
        fun parse(value: String?) = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class Tone { BG, SURFACE, FIELD, INK, MUTED, GREEN, PRIMARY, ON_PRIMARY, PALE, LINE, RED }

data class ThemePalette(val dark: Boolean, private val colors: Map<Tone, Int>) {
    operator fun get(tone: Tone): Int = colors.getValue(tone)
}

object AppTheme {
    const val PREFERENCE = "theme_mode"
    fun mode(context: Context) = ThemeMode.parse(context.applicationContext
        .getSharedPreferences("rumo_private", Context.MODE_PRIVATE).getString(PREFERENCE, null))
    fun palette(context: Context) = palette(mode(context), context.resources.configuration.uiMode)
    fun palette(mode: ThemeMode, uiMode: Int): ThemePalette {
        val dark = when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        return if (dark) night else day
    }
    fun style(context: Context) = if (palette(context).dark) R.style.AppThemeDark else R.style.AppThemeLight
    private fun colors(vararg values: Long) = Tone.entries.zip(values.map { it.toInt() }).toMap()
    private val day = ThemePalette(false, colors(
        0xfff7f8f7, 0xffffffff, 0xfff8faf7, 0xff303331, 0xff617067, 0xff257a55,
        0xff287f59, 0xffffffff, 0xffecf7f0, 0xffdde5df, 0xffa4362e
    ))
    private val night = ThemePalette(true, colors(
        0xff121713, 0xff1c231f, 0xff232a26, 0xffe4ece6, 0xffacb9b0, 0xff78d3aa,
        0xff80d6ad, 0xff0d2b1f, 0xff243d31, 0xff3b4840, 0xffffb4ab
    ))
}
