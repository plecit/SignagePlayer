package com.example.tvsignage.xmds

import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.xmds.models.ActivationStatus
import com.example.tvsignage.xmds.models.RegisterResult
import com.example.tvsignage.xmds.models.RequiredFile
import com.example.tvsignage.xmds.models.ScheduleData
import com.example.tvsignage.xmds.models.ScheduleEvent
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Robust XML response parser for XMDS SOAP methods.
 * Unpacks SOAP Body response elements and parses inner XML payloads.
 */
object XmdsResponseParser {

    private const val TAG = "XmdsResponseParser"

    private fun createPullParser(): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        return factory.newPullParser()
    }

    fun extractInnerResult(soapXml: String, resultTagName: String): String {
        // Try regex first for fast and namespace-agnostic extraction
        val regex = Regex("<(?:[a-zA-Z0-9_]+:)?$resultTagName(?:\\s+[^>]*)?>([\\s\\S]*?)</(?:[a-zA-Z0-9_]+:)?$resultTagName>", RegexOption.IGNORE_CASE)
        val match = regex.find(soapXml)
        if (match != null) {
            val content = match.groupValues[1].trim()
            // Unescape entities if inner XML was encoded
            return unescapeXmlEntities(content)
        }
        return soapXml
    }

    private fun unescapeXmlEntities(str: String): String {
        if (!str.contains("&lt;") && !str.contains("&gt;")) return str
        return str
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    fun parseRegisterDisplay(soapXml: String): RegisterResult {
        val innerXml = extractInnerResult(soapXml, "RegisterDisplayResult")
        val settings = mutableMapOf<String, String>()
        var status = ActivationStatus.UNKNOWN
        var code = 0
        var message = ""

        try {
            val parser = createPullParser()
            parser.setInput(StringReader(innerXml))
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name.lowercase()
                    if (tagName == "display") {
                        val statusAttr = parser.getAttributeValue(null, "status")
                            ?: parser.getAttributeValue(null, "Status") ?: ""
                        val codeAttr = parser.getAttributeValue(null, "code") ?: "0"
                        message = parser.getAttributeValue(null, "message") ?: ""

                        code = codeAttr.toIntOrNull() ?: 0
                        status = when (statusAttr.uppercase()) {
                            "READY", "LICENSED", "ADDED" -> ActivationStatus.READY
                            "WAITING", "UNLICENSED", "PENDING" -> ActivationStatus.WAITING
                            "DISABLED", "BLOCKED" -> ActivationStatus.DISABLED
                            else -> if (code == 100 || code == 200) ActivationStatus.READY else ActivationStatus.WAITING
                        }
                    } else if (tagName == "setting") {
                        val type = parser.getAttributeValue(null, "type") ?: ""
                        val value = parser.nextText()
                        if (type.isNotBlank()) {
                            settings[type] = value
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Error parsing RegisterDisplay XML, checking fallback attributes: ${e.message}")
            if (innerXml.contains("READY", ignoreCase = true)) status = ActivationStatus.READY
            else if (innerXml.contains("WAITING", ignoreCase = true)) status = ActivationStatus.WAITING
        }

        return RegisterResult(status, code, message, settings)
    }

    fun parseRequiredFiles(soapXml: String): Pair<List<RequiredFile>, List<String>> {
        val innerXml = extractInnerResult(soapXml, "RequiredFilesResult")
        val files = mutableListOf<RequiredFile>()
        val purgeFiles = mutableListOf<String>()

        try {
            val parser = createPullParser()
            parser.setInput(StringReader(innerXml))
            var eventType = parser.eventType
            var inPurge = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name.lowercase()
                        if (tagName == "purge") {
                            inPurge = true
                        } else if (tagName == "file") {
                            val id = parser.getAttributeValue(null, "id")?.toLongOrNull() ?: 0L
                            val type = parser.getAttributeValue(null, "type") ?: "media"
                            val size = parser.getAttributeValue(null, "size")?.toLongOrNull() ?: 0L
                            val md5 = parser.getAttributeValue(null, "md5") ?: ""
                            val path = parser.getAttributeValue(null, "path") ?: "$id"

                            if (inPurge) {
                                purgeFiles.add(path)
                            } else {
                                files.add(RequiredFile(id, type, size, md5, path))
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.lowercase() == "purge") {
                            inPurge = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error parsing RequiredFiles XML", e)
        }

        return Pair(files, purgeFiles)
    }

    fun parseSchedule(soapXml: String): ScheduleData {
        val innerXml = extractInnerResult(soapXml, "ScheduleResult")
        var defaultLayoutId = 0L
        val events = mutableListOf<ScheduleEvent>()

        try {
            val parser = createPullParser()
            parser.setInput(StringReader(innerXml))
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name.lowercase()
                    if (tagName == "default") {
                        val idStr = parser.getAttributeValue(null, "layoutid")
                            ?: parser.getAttributeValue(null, "layoutId")
                            ?: parser.getAttributeValue(null, "id")
                        defaultLayoutId = idStr?.toLongOrNull() ?: 0L
                    } else if (tagName == "event") {
                        val id = parser.getAttributeValue(null, "id")?.toLongOrNull() ?: 0L
                        val layoutId = parser.getAttributeValue(null, "layoutid")?.toLongOrNull()
                            ?: parser.getAttributeValue(null, "layoutId")?.toLongOrNull() ?: 0L
                        val fromDt = parser.getAttributeValue(null, "fromdt")
                            ?: parser.getAttributeValue(null, "fromDt") ?: ""
                        val toDt = parser.getAttributeValue(null, "todt")
                            ?: parser.getAttributeValue(null, "toDt") ?: ""
                        val priority = parser.getAttributeValue(null, "priority")?.toIntOrNull() ?: 0

                        if (layoutId > 0) {
                            events.add(ScheduleEvent(id, layoutId, fromDt, toDt, priority))
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error parsing Schedule XML", e)
        }

        return ScheduleData(defaultLayoutId, events)
    }

    fun parseGetFile(soapXml: String): String {
        return extractInnerResult(soapXml, "GetFileResult").trim()
    }

    fun parseGetResource(soapXml: String): String {
        return extractInnerResult(soapXml, "GetResourceResult")
    }
}
