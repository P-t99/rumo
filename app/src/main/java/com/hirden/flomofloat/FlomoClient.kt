package com.hirden.flomofloat

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

object FlomoClient {
    fun send(endpoint: String, draft: Draft): SendResult {
        EndpointPolicy.error(endpoint)?.let { return SendResult(false, it) }
        var connection: HttpsURLConnection? = null
        return try {
            connection = URL(endpoint.trim()).openConnection() as HttpsURLConnection
            connection.apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            val bytes = NoteProtocol.body(draft).toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input ->
                val buffer = ByteArray(65536)
                var total = 0
                while (total < buffer.size) {
                    val n = input.read(buffer, total, buffer.size - total)
                    if (n < 0) break
                    total += n
                }
                String(buffer, 0, total, Charsets.UTF_8)
            }.orEmpty()
            NoteProtocol.response(status, response)
        } catch (_: UnknownHostException) {
            SendResult(false, "无法连接网络，草稿已保留")
        } catch (_: SocketTimeoutException) {
            SendResult(false, "请求超时，请先在 flomo 确认是否已保存，再决定重试", true)
        } catch (_: IOException) {
            SendResult(false, "连接中断，草稿已保留；重试前请确认 flomo 中是否已收到", true)
        } catch (_: Exception) {
            SendResult(false, "发送失败，草稿已保留")
        } finally { connection?.disconnect() }
    }
}

object SendManager {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<(SendResult?) -> Unit>()
    var busy = false
        private set

    fun observe(listener: (SendResult?) -> Unit) { listeners.add(listener) }
    fun remove(listener: (SendResult?) -> Unit) { listeners.remove(listener) }
    fun send(context: Context): String? {
        if (busy) return "正在发送，请稍候"
        val store = LocalStore(context.applicationContext)
        val endpoint = store.api()
        EndpointPolicy.error(endpoint)?.let { return it }
        val snapshot = store.draft()
        if (snapshot.text.isBlank()) return "先写下一点想法吧"
        busy = true
        listeners.forEach { it(null) }
        executor.execute {
            val result = FlomoClient.send(endpoint, snapshot)
            handler.post {
                val safeResult = if (result.success) {
                    runCatching { store.clearIfUnchanged(snapshot) }.fold(
                        onSuccess = { result },
                        onFailure = { SendResult(true, "已存入 flomo，本地草稿未能清除，请勿重复发送") })
                } else result
                store.lastStatus = safeResult.message
                busy = false
                listeners.forEach { it(safeResult) }
            }
        }
        return null
    }
}
