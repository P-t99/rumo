package com.hirden.flomofloat

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.RadioGroup
import androidx.core.graphics.ColorUtils
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.system.container.FxWindowContainer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xhdpi")
class ThemeTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private var activityController: ActivityController<MainActivity>? = null
    private var serviceController: ServiceController<CaptureService>? = null
    @Before fun setup() {
        app.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
    }
    @After fun cleanup() { serviceController?.destroy(); activityController?.pause()?.stop()?.destroy() }
    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }
    private fun activity(): MainActivity {
        activityController = Robolectric.buildActivity(MainActivity::class.java, Intent(app, MainActivity::class.java)).setup()
        return activityController!!.get()
    }
    private fun service(): CaptureService {
        serviceController = Robolectric.buildService(CaptureService::class.java).create()
        return serviceController!!.get().also { call(it, "expand") }
    }
    private fun call(service: CaptureService, name: String) {
        CaptureService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service)
    }
    private fun root(service: CaptureService): FxWindowContainer {
        val editor = CaptureService::class.java.getDeclaredField("editor").apply { isAccessible = true }.get(service) as FxControl
        return editor.contentView!!.parent as FxWindowContainer
    }
    private fun outside(root: View) {
        MotionEvent.obtain(0, 20, MotionEvent.ACTION_OUTSIDE, 20f, 20f, 0).let { root.dispatchTouchEvent(it); it.recycle() }
    }
    @Suppress("DEPRECATION")
    private fun night(context: Context): Configuration {
        val value = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        }
        context.resources.updateConfiguration(value, context.resources.displayMetrics)
        return value
    }
    @Test fun preferenceDefaultsToSystemPersistsAndLeavesOtherDataUntouched() {
        val store = LocalStore(app)
        assertEquals(ThemeMode.SYSTEM, store.themeMode())
        store.saveDraft(Draft("保留草稿", "#临时"))
        store.saveAppearance(FloatingAppearance(OverlayMode.BALL, 46))
        val layout = PanelLayout(310f, 270f, 20f, 15f)
        store.savePanelLayout(false, false, layout)
        val prefs = app.getSharedPreferences("rumo_private", Context.MODE_PRIVATE)
        prefs.edit().putString("api_ciphertext", "opaque-test-ciphertext").commit()
        for (mode in ThemeMode.entries) {
            store.saveThemeMode(mode)
            assertEquals(mode, LocalStore(app).themeMode())
            assertEquals(Draft("保留草稿", "#临时"), store.draft())
            assertEquals(layout, store.panelLayout(false, false))
            assertEquals(FloatingAppearance(OverlayMode.BALL, 46), store.appearance())
            assertEquals("opaque-test-ciphertext", prefs.getString("api_ciphertext", null))
        }
        prefs.edit().putString(AppTheme.PREFERENCE, "UNKNOWN").commit()
        assertEquals(ThemeMode.SYSTEM, store.themeMode())
    }
    @Test fun explicitModesOverrideSystemAndSystemModeTracksBothStates() {
        for (uiMode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            assertTrue(AppTheme.palette(ThemeMode.DARK, uiMode).dark)
            assertFalse(AppTheme.palette(ThemeMode.LIGHT, uiMode).dark)
            assertEquals(uiMode == Configuration.UI_MODE_NIGHT_YES, AppTheme.palette(ThemeMode.SYSTEM, uiMode).dark)
        }
        assertFalse(AppTheme.palette(ThemeMode.SYSTEM, Configuration.UI_MODE_NIGHT_UNDEFINED).dark)
    }
    @Test fun mainSelectorUpdatesColorsWithoutLosingUnsavedSettings() {
        val activity = activity()
        val api = activity.findViewById<EditText>(R.id.api_input)
        val tag = activity.findViewById<EditText>(R.id.preset_one)
        api.setText("https://flomoapp.com/iwh/unsaved-local"); api.setSelection(3, 7)
        tag.setText("未保存标签")
        activity.findViewById<RadioGroup>(R.id.theme_modes).check(R.id.theme_dark); idle()
        assertSame(api, activity.findViewById<EditText>(R.id.api_input))
        assertEquals("https://flomoapp.com/iwh/unsaved-local", api.text.toString())
        assertEquals(3, api.selectionStart); assertEquals(7, api.selectionEnd)
        assertEquals("未保存标签", tag.text.toString())
        assertEquals(ThemeMode.DARK, LocalStore(activity).themeMode())
        assertEquals(AppTheme.palette(activity)[Ui.ink], api.currentTextColor)
        assertEquals(AppTheme.palette(activity)[Ui.bg], activity.window.statusBarColor)
        assertEquals(AppTheme.palette(activity)[Ui.bg], activity.window.navigationBarColor)
        activity.findViewById<RadioGroup>(R.id.theme_modes).check(R.id.theme_light); idle()
        assertFalse(AppTheme.palette(activity).dark)
        assertEquals(AppTheme.palette(activity)[Ui.ink], api.currentTextColor)
    }
    @Test fun switchingAnExpandedNotePreservesDraftSelectionFrameAndInputLease() {
        val service = service(); val root = root(service)
        val field = root.findViewById<EditText>(R.id.note_input)
        field.setText("正在编辑的笔记"); field.setSelection(2, 4)
        val frame = listOf(root.windowParams.x, root.windowParams.y, root.windowParams.width, root.windowParams.height)
        val store = LocalStore(service)
        assertTrue(field.hasFocus())
        store.saveThemeMode(ThemeMode.DARK); idle()
        assertSame(root, root(service)); assertSame(field, root.findViewById<EditText>(R.id.note_input))
        assertEquals("正在编辑的笔记", field.text.toString())
        assertEquals(2, field.selectionStart); assertEquals(4, field.selectionEnd)
        assertEquals(frame, listOf(root.windowParams.x, root.windowParams.y, root.windowParams.width, root.windowParams.height))
        assertTrue(field.hasFocus())
        assertEquals(0, root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        assertEquals(AppTheme.palette(service)[Ui.ink], field.currentTextColor)
        assertNull(field.background)
        outside(root); store.saveThemeMode(ThemeMode.LIGHT); idle()
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertFalse(field.hasFocus())
        assertEquals("正在编辑的笔记", store.draft().text)
    }
    @Test fun systemNightChangeRecolorsAnExpandedNoteWithoutReopeningIt() {
        val service = service(); val root = root(service)
        val field = root.findViewById<EditText>(R.id.note_input)
        field.setText("跟随系统"); field.setSelection(2)
        service.onConfigurationChanged(night(service)); idle()
        assertTrue(AppTheme.palette(service).dark)
        assertSame(root, root(service)); assertSame(field, root.findViewById<EditText>(R.id.note_input))
        assertEquals("跟随系统", field.text.toString()); assertEquals(2, field.selectionStart)
        assertEquals(AppTheme.palette(service)[Ui.ink], field.currentTextColor)
    }
    @Test fun manualLightModeStaysLightUntilSystemModeIsSelected() {
        LocalStore(app).saveThemeMode(ThemeMode.LIGHT)
        val activity = activity(); val field = activity.findViewById<EditText>(R.id.api_input)
        activity.onConfigurationChanged(night(activity)); idle()
        assertFalse(AppTheme.palette(activity).dark)
        assertEquals(AppTheme.palette(activity)[Ui.ink], field.currentTextColor)
        activity.findViewById<RadioGroup>(R.id.theme_modes).check(R.id.theme_system); idle()
        assertTrue(AppTheme.palette(activity).dark)
        assertSame(field, activity.findViewById<EditText>(R.id.api_input))
        assertEquals(AppTheme.palette(activity)[Ui.ink], field.currentTextColor)
    }
    @Test fun floatingSettingsThemeSelectorPreservesUnsavedFieldsAndPassiveFocus() {
        val service = service(); call(service, "showSettings")
        val root = root(service); val api = root.findViewById<EditText>(R.id.api_input)
        api.setText("待保存的地址"); api.setSelection(1, 3)
        root.findViewById<RadioGroup>(R.id.theme_modes).check(R.id.theme_dark); idle()
        assertSame(root, root(service)); assertSame(api, root.findViewById<EditText>(R.id.api_input))
        assertEquals("待保存的地址", api.text.toString()); assertEquals(1, api.selectionStart); assertEquals(3, api.selectionEnd)
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertEquals(AppTheme.palette(service)[Ui.ink], api.currentTextColor)
    }
    @Test fun bothPalettesKeepNormalTextAndPrimaryActionsReadable() {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            val palette = AppTheme.palette(mode, Configuration.UI_MODE_NIGHT_NO)
            for ((foreground, background) in listOf(Ui.ink to Ui.surface, Ui.muted to Ui.surface,
                Ui.ink to Ui.field, Ui.muted to Ui.field, Ui.green to Ui.pale, Ui.onPrimary to Ui.primary)) {
                assertTrue("$mode $foreground on $background", ColorUtils.calculateContrast(palette[foreground], palette[background]) >= 4.5)
            }
        }
    }
}
