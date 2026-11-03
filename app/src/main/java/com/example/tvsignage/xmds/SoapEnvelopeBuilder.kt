package com.example.tvsignage.xmds

import com.example.tvsignage.core.LogEntry
import com.example.tvsignage.stats.StatRecord
import com.example.tvsignage.xmds.models.MediaInventoryItem

/**
 * Builds standard SOAP 1.1/1.2 XML envelopes for XMDS v7 service methods.
 */
object SoapEnvelopeBuilder {

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun wrapEnvelope(methodName: String, bodyContent: String): String {
        return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <$methodName xmlns="urn:xmds">
$bodyContent
    </$methodName>
  </soap:Body>
</soap:Envelope>""".trimIndent()
    }

    fun buildRegisterDisplay(
        serverKey: String,
        hardwareKey: String,
        displayName: String,
        clientType: String,
        clientVersion: String,
        clientCode: Int,
        operatingSystem: String,
        macAddress: String
    ): String {
        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <displayName>${escapeXml(displayName)}</displayName>
      <clientType>${escapeXml(clientType)}</clientType>
      <clientVersion>${escapeXml(clientVersion)}</clientVersion>
      <clientCode>$clientCode</clientCode>
      <operatingSystem>${escapeXml(operatingSystem)}</operatingSystem>
      <macAddress>${escapeXml(macAddress)}</macAddress>
        """.trimIndent()
        return wrapEnvelope("RegisterDisplay", body)
    }

    fun buildRequiredFiles(serverKey: String, hardwareKey: String): String {
        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
        """.trimIndent()
        return wrapEnvelope("RequiredFiles", body)
    }

    fun buildSchedule(serverKey: String, hardwareKey: String): String {
        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
        """.trimIndent()
        return wrapEnvelope("Schedule", body)
    }

    fun buildGetFile(
        serverKey: String,
        hardwareKey: String,
        fileId: Long,
        fileType: String,
        chunkOffset: Long,
        chunkSize: Long
    ): String {
        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <fileId>$fileId</fileId>
      <fileType>${escapeXml(fileType)}</fileType>
      <chunkOffset>$chunkOffset</chunkOffset>
      <chunkSize>$chunkSize</chunkSize>
        """.trimIndent()
        return wrapEnvelope("GetFile", body)
    }

    fun buildGetResource(
        serverKey: String,
        hardwareKey: String,
        layoutId: Long,
        regionId: String,
        mediaId: Long
    ): String {
        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <layoutId>$layoutId</layoutId>
      <regionId>${escapeXml(regionId)}</regionId>
      <mediaId>$mediaId</mediaId>
        """.trimIndent()
        return wrapEnvelope("GetResource", body)
    }

    fun buildMediaInventory(
        serverKey: String,
        hardwareKey: String,
        items: List<MediaInventoryItem>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("<files>")
        for (item in items) {
            val status = if (item.complete && item.md5Valid) "complete" else "downloading"
            sb.append("<file id=\"${item.fileId}\" type=\"${escapeXml(item.type)}\" status=\"$status\" bytesDownloaded=\"${item.bytesDownloaded}\" totalBytes=\"${item.totalBytes}\" />")
        }
        sb.append("</files>")

        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <mediaInventory>${escapeXml(sb.toString())}</mediaInventory>
        """.trimIndent()
        return wrapEnvelope("MediaInventory", body)
    }

    fun buildSubmitStats(
        serverKey: String,
        hardwareKey: String,
        stats: List<StatRecord>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("<statList>")
        for (st in stats) {
            // Note: Xibo CMS 4.5.x fails if scheduleId or layoutId is non-numeric or "undefined"
            val schedId = if (st.scheduleId > 0) st.scheduleId.toString() else "0"
            val layId = if (st.layoutId > 0) st.layoutId.toString() else "0"
            val medId = if (st.mediaId > 0) st.mediaId.toString() else "0"

            sb.append("<stat type=\"${escapeXml(st.type)}\" ")
                .append("fromDt=\"${escapeXml(st.fromDt)}\" ")
                .append("toDt=\"${escapeXml(st.toDt)}\" ")
                .append("layoutId=\"$layId\" ")
                .append("scheduleId=\"$schedId\" ")
            if (st.type == "media") {
                sb.append("mediaId=\"$medId\" ")
            }
            sb.append("duration=\"${st.duration}\" />")
        }
        sb.append("</statList>")

        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <statList>${escapeXml(sb.toString())}</statList>
        """.trimIndent()
        return wrapEnvelope("SubmitStats", body)
    }

    fun buildSubmitLog(
        serverKey: String,
        hardwareKey: String,
        logs: List<LogEntry>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("<logList>")
        for (log in logs) {
            sb.append("<log date=\"${escapeXml(log.timestamp)}\" ")
                .append("category=\"${escapeXml(log.category)}\" ")
                .append("type=\"${escapeXml(log.level)}\" ")
                .append("message=\"${escapeXml(log.message)}\" />")
        }
        sb.append("</logList>")

        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <logList>${escapeXml(sb.toString())}</logList>
        """.trimIndent()
        return wrapEnvelope("SubmitLog", body)
    }

    fun buildNotifyStatus(
        serverKey: String,
        hardwareKey: String,
        statusXml: String
    ): String {
        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <status>${escapeXml(statusXml)}</status>
        """.trimIndent()
        return wrapEnvelope("NotifyStatus", body)
    }

    fun buildSubmitScreenShot(
        serverKey: String,
        hardwareKey: String,
        base64Jpeg: String
    ): String {
        val body = """
      <serverKey>${escapeXml(serverKey)}</serverKey>
      <hardwareKey>${escapeXml(hardwareKey)}</hardwareKey>
      <screenShot>$base64Jpeg</screenShot>
        """.trimIndent()
        return wrapEnvelope("SubmitScreenShot", body)
    }
}
