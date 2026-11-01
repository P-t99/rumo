package com.hirden.flomofloat

object UpdateChannel {
    const val DEFAULT_FEED_URL = "https://rumo.hirkond.online/latest.json"
    private const val LEGACY_FEED_URL = "https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json"

    fun resolve(saved: String?): String = when (saved?.trim()) {
        null, "", LEGACY_FEED_URL -> DEFAULT_FEED_URL
        else -> saved.trim()
    }
}
