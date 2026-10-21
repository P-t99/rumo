package com.hirden.flomofloat

import android.app.Activity
import android.content.Context
import android.widget.RadioGroup
import android.widget.SeekBar
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator
import com.petterp.floatingx.core.FxControl

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceTest {
    private lateinit var activity: Activity
    private lateinit var store: LocalStore
    @Before fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        activity.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        store = LocalStore(activity) { key }
    }
    @Test fun settingsSaveBothModesAndTransparencyWithoutChangingApiOrDraft() {
        val endpoint = "https://flomoapp.com/iwh/fixture/secret"
        store.saveApi(endpoint)
        val draft = Draft("切换模式也保留的草稿", "#标签")
        store.saveDraft(draft)
        val settings = SettingsContent(activity, store) {}
        activity.setContentView(settings)
        settings.findViewById<android.widget.RadioButton>(R.id.mode_ball).performClick()
        settings.findViewById<SeekBar>(R.id.floating_transparency).progress = 65
        assertEquals(FloatingAppearance(OverlayMode.BALL, 65), store.appearance())
        val reopened = SettingsContent(activity, store) {}
        assertTrue(reopened.findViewById<android.widget.RadioButton>(R.id.mode_ball).isChecked)
        assertEquals(65, reopened.findViewById<SeekBar>(R.id.floating_transparency).progress)
        reopened.findViewById<android.widget.RadioButton>(R.id.mode_edge).performClick()
        assertEquals(FloatingAppearance(OverlayMode.EDGE, 65), store.appearance())
        assertEquals(endpoint, store.api()); assertEquals(draft, store.draft())
    }
    @Test fun openSettingsReflectChangesFromAnotherWindow() {
        val settings = SettingsContent(activity, store) {}
        activity.setContentView(settings)
        store.saveAppearance(FloatingAppearance(OverlayMode.BALL, 55))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(settings.findViewById<android.widget.RadioButton>(R.id.mode_ball).isChecked)
        assertEquals(55, settings.findViewById<SeekBar>(R.id.floating_transparency).progress)
    }
    @Test fun transparencyCannotMakeEntryInvisible() {
        assertEquals(.2f, FloatingAppearance(OverlayMode.BALL, 80).opacity, .001f)
        assertEquals(1f, FloatingAppearance(OverlayMode.EDGE, 0).opacity, .001f)
        assertTrue(runCatching { store.saveAppearance(FloatingAppearance(transparency = 100)) }.isFailure)
        val entry = FloatingAppearance(OverlayMode.BALL, 60).createEntry(activity)
        assertEquals(.4f, entry.alpha, .001f)
    }
    @Test fun activeServiceAppliesChangesAndKeepsEntryHiddenWhileEditing() {
        val controller = Robolectric.buildService(CaptureService::class.java).create()
        val service = controller.get()
        fun control(field: String): FxControl? = CaptureService::class.java.getDeclaredField(field).apply { isAccessible = true }.get(service) as FxControl?
        fun call(name: String) { CaptureService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service) }
        try {
            CaptureService::class.java.getDeclaredMethod("installBubble", Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(service, true)
            assertTrue(control("bubble")!!.contentView is EdgeTabView)
            store.saveAppearance(FloatingAppearance(OverlayMode.BALL, 40))
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue(control("bubble")!!.contentView!!.contentDescription.toString().contains("悬浮球"))
            assertEquals(.6f, control("bubble")!!.contentView!!.alpha, .001f)
            store.saveDraft(Draft("不能丢失", "#碎片"))
            call("expand")
            store.saveAppearance(FloatingAppearance(OverlayMode.EDGE, 70))
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertFalse(control("bubble")!!.isShowing)
            assertEquals(1f, control("editor")!!.contentView!!.alpha, .001f)
            assertEquals("不能丢失", store.draft().text)
        } finally { controller.destroy() }
    }
}
