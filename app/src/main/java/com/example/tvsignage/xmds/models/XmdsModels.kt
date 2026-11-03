package com.example.tvsignage.xmds.models

enum class ActivationStatus {
    READY,
    WAITING,
    DISABLED,
    UNKNOWN
}

data class RegisterResult(
    val status: ActivationStatus,
    val code: Int = 0,
    val message: String = "",
    val settings: Map<String, String> = emptyMap()
)

data class RequiredFile(
    val id: Long,
    val type: String, // "media", "layout", "resource", "font"
    val size: Long,
    val md5: String,
    val path: String
)

data class ScheduleEvent(
    val id: Long,
    val layoutId: Long,
    val fromDt: String,
    val toDt: String,
    val priority: Int = 0
)

data class ScheduleData(
    val defaultLayoutId: Long = 0,
    val events: List<ScheduleEvent> = emptyList()
)

data class MediaInventoryItem(
    val fileId: Long,
    val type: String,
    val complete: Boolean,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val md5Valid: Boolean
)
