package com.example.tvsignage.layout

enum class MediaType {
    VIDEO,
    IMAGE,
    WEBPAGE,
    TEXT,
    CLOCK,
    TICKER,
    UNKNOWN
}

data class MediaItem(
    val id: Long,
    val type: MediaType,
    val durationSeconds: Int,
    val uri: String = "",
    val rawContent: String = "",
    val options: Map<String, String> = emptyMap()
)

data class LayoutRegion(
    val id: String,
    val width: Int,
    val height: Int,
    val top: Int,
    val left: Int,
    val zIndex: Int = 0,
    val mediaItems: List<MediaItem> = emptyList()
)

data class Layout(
    val id: Long,
    val width: Int = 1920,
    val height: Int = 1080,
    val backgroundColor: String = "#000000",
    val regions: List<LayoutRegion> = emptyList()
)
