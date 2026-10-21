package com.hirden.flomofloat

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LocalStore(context: Context, private val keySource: (() -> SecretKey)? = null) {
    private val prefs = context.applicationContext.getSharedPreferences("rumo_private", Context.MODE_PRIVATE)
    var apiReadFailed: Boolean = false
        private set

    init {
        // Migrate v0.1 tags once, without touching the API ciphertext or unfinished text.
        if (!prefs.contains("default_tags")) {
            check(prefs.edit().putString("default_tags", prefs.getString("draft_tags", "#碎片")).commit())
        }
    }
    private fun key(): SecretKey {
        keySource?.let { return it() }
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }

    fun api(): String {
        val encrypted = prefs.getString("api_ciphertext", null) ?: return ""
        return try {
            val parts = encrypted.split(":", limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8).also { apiReadFailed = false }
        } catch (_: Exception) { apiReadFailed = true; "" }
    }

    fun saveApi(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(prefs.edit().putString("api_ciphertext", "$iv:$encrypted").commit()) { "Local storage unavailable" }
        apiReadFailed = false
    }

    fun forgetApi() {
        check(prefs.edit().remove("api_ciphertext").commit())
        apiReadFailed = false
    }
    fun appearance(): FloatingAppearance = FloatingAppearance(
        OverlayMode.entries.firstOrNull { it.name == prefs.getString("overlay_mode", "EDGE") } ?: OverlayMode.EDGE,
        prefs.getInt("overlay_transparency", 20).coerceIn(0, 80)
    )
    fun saveAppearance(value: FloatingAppearance) {
        require(value.transparency in 0..80)
        prefs.edit().putString("overlay_mode", value.mode.name).putInt("overlay_transparency", value.transparency).apply()
    }
    fun observeAppearance(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) { prefs.registerOnSharedPreferenceChangeListener(listener) }
    fun removeAppearanceObserver(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    private fun panelKey(settings: Boolean, landscape: Boolean) = "panel_${if (settings) "settings" else "note"}_${if (landscape) "landscape" else "portrait"}_"
    fun panelLayout(settings: Boolean, landscape: Boolean): PanelLayout? {
        val prefix = panelKey(settings, landscape)
        if (!prefs.contains(prefix + "width")) return null
        return runCatching {
            PanelLayout(prefs.getFloat(prefix + "width", 0f), prefs.getFloat(prefix + "height", 0f),
                prefs.getFloat(prefix + "x", 0f), prefs.getFloat(prefix + "y", 0f)).takeIf { it.isValid() }
        }.getOrNull()
    }
    fun savePanelLayout(settings: Boolean, landscape: Boolean, value: PanelLayout) {
        require(value.isValid())
        val prefix = panelKey(settings, landscape)
        check(prefs.edit().putFloat(prefix + "width", value.width).putFloat(prefix + "height", value.height)
            .putFloat(prefix + "x", value.x).putFloat(prefix + "y", value.y).commit())
    }
    fun resetPanelLayouts() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("panel_") }.forEach { editor.remove(it) }
        check(editor.putInt("layout_reset_revision", prefs.getInt("layout_reset_revision", 0) + 1).commit())
    }
    fun defaultTags(): String = prefs.getString("default_tags", null)
        ?: prefs.getString("draft_tags", "#碎片").orEmpty()
    fun presets(): List<String> = (0..2).map { prefs.getString("preset_$it", listOf("工作", "灵感", "阅读")[it]).orEmpty() }
    fun saveTags(presets: List<String>, defaults: String) {
        require(presets.size == 3)
        val normalized = presets.map { NoteProtocol.tags(it).removePrefix("#") }
        require(normalized.all { it.isNotBlank() && !it.contains(" ") && it.length <= 24 })
        require(normalized.distinct().size == 3)
        val editor = prefs.edit().putString("default_tags", NoteProtocol.tags(defaults))
        normalized.forEachIndexed { i, tag -> editor.putString("preset_$i", tag) }
        // Apply to the next blank note; never replace tags attached to an unfinished note.
        if (draft().text.isBlank()) editor.putString("draft_tags", NoteProtocol.tags(defaults))
        check(editor.commit())
    }
    fun draft(): Draft = Draft(prefs.getString("draft_text", "") ?: "", prefs.getString("draft_tags", defaultTags()) ?: "")
    fun saveDraft(draft: Draft) {
        check(prefs.edit().putString("draft_text", draft.text).putString("draft_tags", draft.tags).commit())
    }
    fun clearIfUnchanged(sent: Draft) {
        if (draft() == sent) {
            val defaults = defaultTags()
            check(prefs.edit().putString("default_tags", defaults).putString("draft_text", "").putString("draft_tags", defaults).commit())
        }
    }
    var lastStatus: String
        get() = prefs.getString("last_status", "") ?: ""
        set(value) { prefs.edit().putString("last_status", value).apply() }

    companion object { private const val KEY_ALIAS = "hirden_flomofloat_api_v1" }
}
