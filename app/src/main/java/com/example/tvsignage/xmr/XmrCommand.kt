package com.example.tvsignage.xmr

import com.example.tvsignage.core.SystemLogger
import org.json.JSONObject

enum class XmrAction {
    COLLECT_NOW,
    SCREENSHOT,
    PURGE_ALL,
    CHANGE_LAYOUT,
    REKEY,
    UNKNOWN
}

data class XmrCommand(
    val action: XmrAction,
    val layoutId: Long = 0L,
    val payload: String = ""
)

object XmrMessageParser {

    fun parse(rawMessage: String): XmrCommand {
        try {
            // Check if JSON format
            if (rawMessage.trim().startsWith("{")) {
                val json = JSONObject(rawMessage)
                val actionStr = json.optString("action", json.optString("type", ""))
                val action = when (actionStr.lowercase()) {
                    "collect", "collectnow" -> XmrAction.COLLECT_NOW
                    "screenshot", "screen" -> XmrAction.SCREENSHOT
                    "purge", "purgeall" -> XmrAction.PURGE_ALL
                    "changelayout" -> XmrAction.CHANGE_LAYOUT
                    "rekey" -> XmrAction.REKEY
                    else -> XmrAction.UNKNOWN
                }
                val layoutId = json.optLong("layoutId", 0L)
                return XmrCommand(action, layoutId, rawMessage)
            }

            // Plain text command
            val action = when (rawMessage.trim().lowercase()) {
                "collectnow", "collect" -> XmrAction.COLLECT_NOW
                "screenshot" -> XmrAction.SCREENSHOT
                "purgeall" -> XmrAction.PURGE_ALL
                else -> XmrAction.UNKNOWN
            }
            return XmrCommand(action, 0L, rawMessage)
        } catch (e: Exception) {
            SystemLogger.w("XmrMessageParser", "Failed to parse XMR message: ${e.message}")
            return XmrCommand(XmrAction.UNKNOWN, 0L, rawMessage)
        }
    }
}
