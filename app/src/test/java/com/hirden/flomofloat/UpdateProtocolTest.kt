package com.hirden.flomofloat

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class UpdateProtocolTest {
    private val raw = """{"versionCode":3,"versionName":"0.3.0","apkUrl":"https://example.org/app.apk","sha256":"${"a".repeat(64)}","notes":"更新说明","minSdk":26}"""
    @Test fun releaseMetadataRoundTrips() {
        val release = AppRelease.parse(raw)
        assertEquals(3, release.code)
        assertEquals(release, AppRelease.parse(release.json()))
    }
    @Test fun invalidUrlsAndHashesAreRejected() {
        for (url in listOf("http://example.org/a.apk", "https://user:token@example.org/a", "file:///a", "https://example.org/a#b", "https://example.org:8080/a")) {
            assertFalse(url, AppRelease.validHttps(url))
            assertTrue(runCatching { AppRelease.parse(raw.replace("https://example.org/app.apk", url)) }.isFailure)
        }
        assertTrue(runCatching { AppRelease.parse(raw.replace("a".repeat(64), "invalid")) }.isFailure)
    }
    @Test fun hashUsesActualFileBytes() {
        val file = File.createTempFile("rumo-check", ".bin")
        try {
            file.writeText("abc")
            assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", AppUpdates.sha256(file))
        } finally { file.delete() }
    }
}
