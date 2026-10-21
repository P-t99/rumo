package com.hirden.flomofloat

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NoteProtocolTest {
    @Test fun acceptsOnlyOfficialHttpsWebhookEndpoints() {
        assertNull(EndpointPolicy.error(" https://flomoapp.com/iwh/user/secret "))
        assertNull(EndpointPolicy.error("https://flomo.app/iwh/user/secret"))
        listOf("", "http://flomoapp.com/iwh/a/b", "https://flomoapp.com.evil.test/iwh/a/b",
            "https://evil.test/iwh/a/b", "https://flomoapp.com/", "https://flomoapp.com/iwh/",
            "https://attacker@flomoapp.com/iwh/a/b", "https://flomoapp.com:8443/iwh/a/b")
            .forEach { assertNotNull(it, EndpointPolicy.error(it)) }
    }
    @Test fun payloadPreservesChineseQuotesNewlinesAndEmoji() {
        val note = Draft("测试\n他说：\"你好\"，反斜线\\ 和 🌱", "工作 #灵感 #工作")
        val actual = JSONObject(NoteProtocol.body(note)).getString("content")
        assertEquals("测试\n他说：\"你好\"，反斜线\\ 和 🌱\n\n#工作 #灵感", actual)
    }
    @Test fun onlyConfirmedApiSuccessClearsANote() {
        assertTrue(NoteProtocol.response(200, "{\"code\":0,\"message\":\"成功\"}").success)
        assertFalse(NoteProtocol.response(200, "{\"code\":-1,\"message\":\"API 无效\"}").success)
        assertFalse(NoteProtocol.response(500, "{\"code\":0}").success)
        assertFalse(NoteProtocol.response(200, "<html>unexpected</html>").success)
        assertFalse(NoteProtocol.response(302, "").success)
    }
    @Test fun tagsAreOptionalAndRepeatedTagsAreRemoved() {
        assertEquals("#工作 #阅读/访谈", NoteProtocol.tags("#工作  工作\n#阅读/访谈"))
        assertEquals("一段想法", NoteProtocol.content(Draft("  一段想法  ", "")))
    }
    @Test fun remoteErrorMessagesDoNotDisplayWebhookUrls() {
        assertFalse(NoteProtocol.response(400, "{\"message\":\"invalid https://flomoapp.com/iwh/u/secret\"}").message.contains("secret"))
    }
}
