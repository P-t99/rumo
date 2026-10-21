package com.hirden.flomofloat

import org.junit.Assert.*
import org.junit.Test

class PanelGeometryTest {
    @Test fun keyboardProjectionLeavesContextVisibleAndCanRestoreOriginalLayout() {
        val preference = PanelLayout(320f, 480f, 30f, 350f)
        val full = PanelViewport(10, 30, 410, 900)
        val before = PanelGeometry.fit(preference, full, 1f)
        val aboveKeyboard = PanelViewport(10, 30, 410, 450, keyboard = true)
        val fitted = PanelGeometry.fit(preference, aboveKeyboard, 1f)
        assertTrue(fitted.y >= aboveKeyboard.top)
        assertTrue(fitted.y + fitted.height <= aboveKeyboard.bottom)
        assertTrue(fitted.height < aboveKeyboard.height)
        assertEquals(before, PanelGeometry.fit(preference, full, 1f))
        assertEquals(480f, preference.height, 0f)
    }
    @Test fun oversizedAndOffscreenLayoutRemainsReachableAfterDisplaySizeChange() {
        val fitted = PanelGeometry.fit(PanelLayout(2000f, 3000f, 900f, 2000f), PanelViewport(10, 20, 330, 600), 1f)
        assertEquals(PanelFrame(10, 20, 320, 580), fitted)
    }
    @Test fun narrowLandscapeAndLargeDensityNeverProduceAnOffscreenWindow() {
        for (density in listOf(1f, 2f, 3.5f)) for (keyboard in listOf(false, true)) {
            val viewport = PanelViewport(10, 20, 230, 150, keyboard)
            val fitted = PanelGeometry.fit(PanelLayout(1f, 1f, 100f, 100f), viewport, density)
            assertTrue(fitted.width in 1..viewport.width)
            assertTrue(fitted.height in 1..viewport.height)
            assertTrue(fitted.x + fitted.width <= viewport.right)
            assertTrue(fitted.y + fitted.height <= viewport.bottom)
        }
    }
}
