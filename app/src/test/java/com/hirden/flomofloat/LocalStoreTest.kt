package com.hirden.flomofloat

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LocalStoreTest {
    private lateinit var context: Context
    private lateinit var key: SecretKey
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }
    private fun store() = LocalStore(context) { key }
    @Test fun apiSurvivesNewStoreInstanceAndIsEncryptedAtRest() {
        val api = "https://flomoapp.com/iwh/test/secret-token"
        store().saveApi(api)
        assertEquals(api, store().api())
        val values = context.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).all.values.joinToString()
        assertFalse(values.contains("secret-token"))
        assertFalse(values.contains("flomoapp.com"))
        store().forgetApi()
        assertEquals("", store().api())
    }
    @Test fun draftSurvivesReopeningAndSuccessUsesConfiguredDefaults() {
        val draft = Draft("没有发送的中文草稿", "#工作")
        store().saveDraft(draft)
        assertEquals(draft, store().draft())
        store().clearIfUnchanged(draft)
        assertEquals(Draft("", "#碎片"), store().draft())
    }
    @Test fun legacyUpgradeKeepsApiDraftAndLastTagsAsDefaults() {
        context.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit()
            .putString("draft_text", "升级前的草稿").putString("draft_tags", "#旧标签").commit()
        assertEquals("#旧标签", store().defaultTags())
        assertEquals(Draft("升级前的草稿", "#旧标签"), store().draft())
        store().saveDraft(Draft("临时标签", "#临时"))
        store().clearIfUnchanged(Draft("临时标签", "#临时"))
        assertEquals("#旧标签", store().draft().tags)
    }
    @Test fun editedPresetsAndDefaultsSurviveReopeningWithoutChangingActiveDraft() {
        store().saveDraft(Draft("正在写", "#临时"))
        store().saveTags(listOf("项目", "想法", "随读"), "工作 #归档 #工作")
        assertEquals(listOf("项目", "想法", "随读"), store().presets())
        assertEquals("#工作 #归档", store().defaultTags())
        assertEquals(Draft("正在写", "#临时"), store().draft())
        store().clearIfUnchanged(Draft("正在写", "#临时"))
        assertEquals(Draft("", "#工作 #归档"), store().draft())
    }
    @Test fun changedDefaultsApplyImmediatelyToBlankNote() {
        store().saveTags(listOf("A", "B", "C"), "#自动")
        assertEquals(Draft("", "#自动"), store().draft())
    }
    @Test fun completionCannotEraseANewerDraft() {
        val previous = Draft("旧想法", "#工作")
        val newer = Draft("新的想法", "#灵感")
        store().saveDraft(previous)
        store().saveDraft(newer)
        store().clearIfUnchanged(previous)
        assertEquals(newer, store().draft())
    }
    @Test fun unreadableCiphertextIsReportedWithoutDiscardingDraft() {
        store().saveDraft(Draft("保留这条", ""))
        context.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().putString("api_ciphertext", "broken").commit()
        val reopened = store()
        assertEquals("", reopened.api())
        assertTrue(reopened.apiReadFailed)
        assertEquals("保留这条", reopened.draft().text)
    }
}
