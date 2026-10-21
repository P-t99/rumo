package com.hirden.flomofloat

import org.json.JSONObject
import java.net.URI
import java.util.Locale

object EndpointPolicy {
    fun error(value: String): String? {
        val url = value.trim()
        if (url.isEmpty()) return "请先填写 flomo API 地址"
        val uri = try { URI(url) } catch (_: Exception) { return "API 地址格式不正确，请完整复制" }
        if (uri.scheme?.lowercase(Locale.ROOT) != "https") return "API 地址需要以 https:// 开头"
        if (uri.host?.lowercase(Locale.ROOT) !in setOf("flomoapp.com", "flomo.app"))
            return "请填写 flomo 官方的 incoming webhook 地址"
        if (uri.userInfo != null || uri.port !in setOf(-1, 443) || uri.fragment != null)
            return "API 地址格式不正确，请完整复制"
        if (!uri.path.orEmpty().startsWith("/iwh/") || uri.path.orEmpty().removePrefix("/iwh/").isBlank())
            return "请在 flomo 设置 → API 中复制完整的 /iwh/ 地址"
        return null
    }
}

data class Draft(val text: String, val tags: String)
data class SendResult(val success: Boolean, val message: String, val uncertain: Boolean = false)

object NoteProtocol {
    fun tags(value: String): String = value.split(Regex("\\s+"))
        .map { it.trim().trimStart('#') }.filter { it.isNotBlank() }
        .distinct().joinToString(" ") { "#$it" }

    fun content(draft: Draft): String {
        val text = draft.text.trim()
        val tags = tags(draft.tags)
        return if (tags.isEmpty()) text else "$text\n\n$tags"
    }

    fun body(draft: Draft): String = JSONObject().put("content", content(draft)).toString()

    fun response(httpStatus: Int, body: String): SendResult {
        if (httpStatus in 300..399) return SendResult(false, "接口发生重定向，请重新复制 flomo API 地址")
        val json = runCatching { JSONObject(body) }.getOrNull()
        if (httpStatus in 200..299 && json?.optInt("code", Int.MIN_VALUE) == 0)
            return SendResult(true, "已存入 flomo")
        val detail = json?.optString("message", "")?.takeIf { it.isNotBlank() }
            ?.replace(Regex("https?://\\S+"), "[接口地址]")?.take(160)
        return SendResult(false, detail ?: if (httpStatus in 200..299)
            "未收到 flomo 的成功确认，草稿已保留" else "发送失败（HTTP $httpStatus），草稿已保留")
    }
}
