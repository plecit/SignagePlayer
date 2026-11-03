package com.example.tvsignage

import com.example.tvsignage.xmds.XmdsResponseParser
import com.example.tvsignage.xmds.models.ActivationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XmdsResponseParserTest {

    private fun loadFixture(filename: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("fixtures/$filename")
            ?: throw IllegalArgumentException("Fixture not found: fixtures/$filename")
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun testParseRegisterDisplayReady() {
        val xml = loadFixture("register_display_ready.xml")
        val result = XmdsResponseParser.parseRegisterDisplay(xml)

        assertEquals(ActivationStatus.READY, result.status)
        assertEquals(100, result.code)
        assertEquals("60", result.settings["collectInterval"])
        assertEquals("1", result.settings["statsEnabled"])
        assertEquals("tcp://192.168.1.100:50001", result.settings["xmrChannel"])
        assertEquals("Asia/Jakarta", result.settings["timezone"])
    }

    @Test
    fun testParseRegisterDisplayWaiting() {
        val xml = loadFixture("register_display_waiting.xml")
        val result = XmdsResponseParser.parseRegisterDisplay(xml)

        assertEquals(ActivationStatus.WAITING, result.status)
        assertEquals(0, result.code)
        assertEquals("30", result.settings["collectInterval"])
    }

    @Test
    fun testParseRequiredFiles() {
        val xml = loadFixture("required_files.xml")
        val (files, purge) = XmdsResponseParser.parseRequiredFiles(xml)

        assertEquals(4, files.size)
        assertEquals(1, purge.size)

        val layoutFile = files.find { it.id == 1L }
        assertNotNull(layoutFile)
        assertEquals("layout", layoutFile?.type)
        assertEquals("1.xlf", layoutFile?.path)
        assertEquals(1450L, layoutFile?.size)
        assertEquals("a1b2c3d4e5f60718293a4b5c6d7e8f90", layoutFile?.md5)

        val videoFile = files.find { it.id == 102L }
        assertNotNull(videoFile)
        assertEquals("media", videoFile?.type)
        assertEquals("102.mp4", videoFile?.path)

        assertEquals("old_promo.mp4", purge[0])
    }

    @Test
    fun testParseSchedule() {
        val xml = loadFixture("schedule.xml")
        val scheduleData = XmdsResponseParser.parseSchedule(xml)

        assertEquals(1L, scheduleData.defaultLayoutId)
        assertEquals(2, scheduleData.events.size)

        val ev1 = scheduleData.events[0]
        assertEquals(10L, ev1.id)
        assertEquals(2L, ev1.layoutId)
        assertEquals("2026-11-01 00:00:00", ev1.fromDt)
        assertEquals(5, ev1.priority)

        val ev2 = scheduleData.events[1]
        assertEquals(11L, ev2.id)
        assertEquals(3L, ev2.layoutId)
        assertEquals(10, ev2.priority)
    }
}
