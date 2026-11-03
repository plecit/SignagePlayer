package com.example.tvsignage

import com.example.tvsignage.layout.MediaType
import com.example.tvsignage.layout.XlfParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class XlfParserTest {

    private fun loadFixture(filename: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("fixtures/$filename")
            ?: throw IllegalArgumentException("Fixture not found: fixtures/$filename")
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun testParseSampleLayout() {
        val xml = loadFixture("layout_sample.xlf")
        val layout = XlfParser.parse(xml, 1L)

        assertNotNull(layout)
        assertEquals(1L, layout?.id)
        assertEquals(1920, layout?.width)
        assertEquals(1080, layout?.height)
        assertEquals("#000000", layout?.backgroundColor)
        assertEquals(3, layout?.regions?.size)

        // Region 1: Video
        val r1 = layout?.regions?.find { it.id == "r_main_video" }
        assertNotNull(r1)
        assertEquals(1280, r1?.width)
        assertEquals(720, r1?.height)
        assertEquals(0, r1?.left)
        assertEquals(0, r1?.top)
        assertEquals(1, r1?.mediaItems?.size)
        assertEquals(MediaType.VIDEO, r1?.mediaItems?.first()?.type)
        assertEquals(15, r1?.mediaItems?.first()?.durationSeconds)
        assertEquals("102.mp4", r1?.mediaItems?.first()?.uri)

        // Region 2: Image
        val r2 = layout?.regions?.find { it.id == "r_side_banner" }
        assertNotNull(r2)
        assertEquals(640, r2?.width)
        assertEquals(720, r2?.height)
        assertEquals(1280, r2?.left)
        assertEquals(10, r2?.mediaItems?.first()?.durationSeconds)
        assertEquals(MediaType.IMAGE, r2?.mediaItems?.first()?.type)
        assertEquals("101.jpg", r2?.mediaItems?.first()?.uri)

        // Region 3: Text / Ticker
        val r3 = layout?.regions?.find { it.id == "r_bottom_ticker" }
        assertNotNull(r3)
        assertEquals(1920, r3?.width)
        assertEquals(360, r3?.height)
        assertEquals(720, r3?.top)
        assertEquals(MediaType.TEXT, r3?.mediaItems?.first()?.type)
    }
}
