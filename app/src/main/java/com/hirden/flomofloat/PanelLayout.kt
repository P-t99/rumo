package com.hirden.flomofloat

import kotlin.math.roundToInt

/** User preference in dp. Keyboard avoidance is a projection, never a saved preference. */
data class PanelLayout(val width: Float, val height: Float, val x: Float, val y: Float) {
    fun isValid() = listOf(width, height, x, y).all { it.isFinite() } && width > 0 && height > 0 && x >= 0 && y >= 0
}

data class PanelViewport(val left: Int, val top: Int, val right: Int, val bottom: Int, val keyboard: Boolean = false) {
    val width get() = (right - left).coerceAtLeast(1)
    val height get() = (bottom - top).coerceAtLeast(1)
}

data class PanelFrame(val x: Int, val y: Int, val width: Int, val height: Int)

object PanelGeometry {
    fun default(viewport: PanelViewport, density: Float, settings: Boolean): PanelLayout {
        val width = minOf(380f, viewport.width / density)
        val height = minOf(if (settings) 460f else 360f, viewport.height / density * .55f)
        return PanelLayout(width, height, (viewport.width / density - width) / 2, 20f)
    }

    fun fit(layout: PanelLayout, viewport: PanelViewport, density: Float): PanelFrame {
        val maxHeight = if (viewport.keyboard) (viewport.height * .8f).roundToInt().coerceAtLeast(1) else viewport.height
        val width = (layout.width * density).roundToInt().coerceIn(minOf((280 * density).roundToInt(), viewport.width), viewport.width)
        val height = (layout.height * density).roundToInt().coerceIn(minOf((180 * density).roundToInt(), maxHeight), maxHeight)
        return PanelFrame(
            viewport.left + (layout.x * density).roundToInt().coerceIn(0, viewport.width - width),
            viewport.top + (layout.y * density).roundToInt().coerceIn(0, viewport.height - height), width, height
        )
    }
}
