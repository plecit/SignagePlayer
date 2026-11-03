package com.example.tvsignage.layout

import com.example.tvsignage.core.SystemLogger
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.StringReader

/**
 * Parses Xibo XLF (XML Layout Format) layout files.
 */
object XlfParser {

    private const val TAG = "XlfParser"

    private fun createPullParser(): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        return factory.newPullParser()
    }

    fun parse(file: File, layoutId: Long): Layout? {
        if (!file.exists()) return null
        return try {
            FileInputStream(file).use { fis ->
                parse(fis, layoutId)
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to parse XLF file: ${file.name}", e)
            null
        }
    }

    fun parse(xmlString: String, layoutId: Long): Layout? {
        return try {
            val parser = createPullParser()
            parser.setInput(StringReader(xmlString))
            parseInternal(parser, layoutId)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to parse XLF xml string", e)
            null
        }
    }

    fun parse(inputStream: InputStream, layoutId: Long): Layout? {
        return try {
            val parser = createPullParser()
            parser.setInput(inputStream, "UTF-8")
            parseInternal(parser, layoutId)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to parse XLF input stream", e)
            null
        }
    }

    private fun parseInternal(parser: XmlPullParser, layoutId: Long): Layout {
        var layoutWidth = 1920
        var layoutHeight = 1080
        var bgcolor = "#000000"
        val regions = mutableListOf<LayoutRegion>()

        var currentRegionId = ""
        var currentRegionWidth = 1920
        var currentRegionHeight = 1080
        var currentRegionTop = 0
        var currentRegionLeft = 0
        var currentRegionZIndex = 0
        val currentMediaList = mutableListOf<MediaItem>()

        var currentMediaId = 0L
        var currentMediaType = MediaType.UNKNOWN
        var currentMediaDuration = 10
        val currentOptions = mutableMapOf<String, String>()
        var currentRawContent = ""
        var inOptions = false
        var inRaw = false

        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase() ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "layout" -> {
                            layoutWidth = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: 1920
                            layoutHeight = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: 1080
                            bgcolor = parser.getAttributeValue(null, "bgcolor") ?: "#000000"
                        }
                        "region" -> {
                            currentRegionId = parser.getAttributeValue(null, "id") ?: "r_${regions.size}"
                            currentRegionWidth = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: layoutWidth
                            currentRegionHeight = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: layoutHeight
                            currentRegionTop = parser.getAttributeValue(null, "top")?.toIntOrNull() ?: 0
                            currentRegionLeft = parser.getAttributeValue(null, "left")?.toIntOrNull() ?: 0
                            currentRegionZIndex = parser.getAttributeValue(null, "zIndex")?.toIntOrNull()
                                ?: parser.getAttributeValue(null, "zindex")?.toIntOrNull() ?: 0
                            currentMediaList.clear()
                        }
                        "media" -> {
                            currentMediaId = parser.getAttributeValue(null, "id")?.toLongOrNull() ?: 0L
                            val typeStr = parser.getAttributeValue(null, "type")?.lowercase() ?: ""
                            currentMediaDuration = parser.getAttributeValue(null, "duration")?.toIntOrNull() ?: 10
                            currentMediaType = when (typeStr) {
                                "video" -> MediaType.VIDEO
                                "image" -> MediaType.IMAGE
                                "webpage", "embedded" -> MediaType.WEBPAGE
                                "text", "richtext" -> MediaType.TEXT
                                "clock" -> MediaType.CLOCK
                                "ticker" -> MediaType.TICKER
                                else -> MediaType.UNKNOWN
                            }
                            currentOptions.clear()
                            currentRawContent = ""
                        }
                        "options" -> inOptions = true
                        "raw" -> inRaw = true
                        else -> {
                            if (inOptions) {
                                val optKey = tag
                                val optVal = parser.nextText()
                                currentOptions[optKey] = optVal
                            } else if (inRaw) {
                                currentRawContent = parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tag) {
                        "options" -> inOptions = false
                        "raw" -> inRaw = false
                        "media" -> {
                            val uri = currentOptions["uri"] ?: currentOptions["file"] ?: "$currentMediaId"
                            currentMediaList.add(
                                MediaItem(
                                    id = currentMediaId,
                                    type = currentMediaType,
                                    durationSeconds = currentMediaDuration,
                                    uri = uri,
                                    rawContent = currentRawContent,
                                    options = HashMap(currentOptions)
                                )
                            )
                        }
                        "region" -> {
                            regions.add(
                                LayoutRegion(
                                    id = currentRegionId,
                                    width = currentRegionWidth,
                                    height = currentRegionHeight,
                                    top = currentRegionTop,
                                    left = currentRegionLeft,
                                    zIndex = currentRegionZIndex,
                                    mediaItems = ArrayList(currentMediaList)
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return Layout(
            id = layoutId,
            width = layoutWidth,
            height = layoutHeight,
            backgroundColor = bgcolor,
            regions = regions
        )
    }
}
