package com.example.myapplication

import org.junit.Test
import org.junit.Assert.*

class M3UParserTest {

    @Test
    fun testParseM3U() {
        val parser = M3UParser()
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="test1" tvg-name="Test Channel 1" tvg-logo="logo1.jpg" group-title="General",Test Channel 1
            http://example.com/stream1.m3u8
            #EXTINF:-1 tvg-id="test2" tvg-name="Test Channel 2" tvg-logo="logo2.jpg" group-title="News",Test Channel 2
            http://example.com/stream2.m3u8
        """.trimIndent()

        val channels = parser.parseM3U(m3uContent)
        
        assertEquals(2, channels.size)
        assertEquals("test1", channels[0].id)
        assertEquals("Test Channel 1", channels[0].name)
        assertEquals("logo1.jpg", channels[0].logoUrl)
        assertEquals("General", channels[0].groupTitle)
        assertEquals("http://example.com/stream1.m3u8", channels[0].streamUrl)
    }

    @Test
    fun testParseM3UWithQuotes() {
        val parser = M3UParser()
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="test1" tvg-name="Test Channel" tvg-logo="logo.jpg" group-title="General",Test Channel
            http://example.com/stream.m3u8
        """.trimIndent()

        val channels = parser.parseM3U(m3uContent)
        
        assertEquals(1, channels.size)
        assertEquals("test1", channels[0].id)
        assertEquals("Test Channel", channels[0].name)
        assertEquals("logo.jpg", channels[0].logoUrl)
        assertEquals("General", channels[0].groupTitle)
    }

    @Test
    fun testParseEmptyM3U() {
        val parser = M3UParser()
        val channels = parser.parseM3U("")
        
        assertEquals(0, channels.size)
    }
}