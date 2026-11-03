package com.example.tvsignage

import com.example.tvsignage.stats.StatRecord
import com.example.tvsignage.xmds.SoapEnvelopeBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoapEnvelopeBuilderTest {

    @Test
    fun testBuildRegisterDisplayEnvelope() {
        val xml = SoapEnvelopeBuilder.buildRegisterDisplay(
            serverKey = "secret123",
            hardwareKey = "uuid-1234-5678",
            displayName = "TV-Lobby",
            clientType = "android",
            clientVersion = "1.0.0",
            clientCode = 100,
            operatingSystem = "Android TV 12",
            macAddress = "02:00:00:00:00:00"
        )

        assertTrue(xml.contains("<serverKey>secret123</serverKey>"))
        assertTrue(xml.contains("<hardwareKey>uuid-1234-5678</hardwareKey>"))
        assertTrue(xml.contains("<displayName>TV-Lobby</displayName>"))
        assertTrue(xml.contains("<clientType>android</clientType>"))
        assertTrue(xml.contains("xmlns=\"urn:xmds\""))
    }

    @Test
    fun testBuildSubmitStatsNoUndefinedIds() {
        val stats = listOf(
            StatRecord(
                id = 1,
                type = "layout",
                layoutId = 12,
                scheduleId = 5,
                mediaId = 0,
                fromDt = "2026-11-03 10:00:00",
                toDt = "2026-11-03 10:00:30",
                duration = 30
            ),
            StatRecord(
                id = 2,
                type = "media",
                layoutId = 12,
                scheduleId = 5,
                mediaId = 101,
                fromDt = "2026-11-03 10:00:00",
                toDt = "2026-11-03 10:00:15",
                duration = 15
            )
        )

        val xml = SoapEnvelopeBuilder.buildSubmitStats("key", "hwkey", stats)

        // Ensure string "undefined" is NEVER present (protects against Xibo CMS 4.5.x bug)
        assertFalse(xml.contains("undefined"))
        assertTrue(xml.contains("layoutId=\"12\""))
        assertTrue(xml.contains("scheduleId=\"5\""))
        assertTrue(xml.contains("mediaId=\"101\""))
    }
}
