package com.example.tvsignage.player

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.files.LocalStorageManager
import com.example.tvsignage.layout.Layout
import com.example.tvsignage.layout.LayoutRegion
import com.example.tvsignage.layout.MediaItem
import com.example.tvsignage.layout.MediaType
import com.example.tvsignage.stats.ProofOfPlayEngine

class LayoutRenderer(
    private val context: Context,
    private val storageManager: LocalStorageManager,
    private val proofOfPlayEngine: ProofOfPlayEngine
) {

    private val handler = Handler(Looper.getMainLooper())
    private val activeVideoRenderers = mutableListOf<VideoRegionRenderer>()
    private val activeWebRenderers = mutableListOf<WebWidgetRenderer>()
    private val activeRunnables = mutableListOf<Runnable>()

    fun renderLayout(
        layout: Layout,
        container: ViewGroup,
        scheduleId: Long,
        onLayoutCycleComplete: (() -> Unit)? = null
    ) {
        clear(container)
        proofOfPlayEngine.onLayoutStart(layout.id, scheduleId)

        try {
            val bg = Color.parseColor(layout.backgroundColor)
            container.setBackgroundColor(bg)
        } catch (e: Exception) {
            container.setBackgroundColor(Color.BLACK)
        }

        container.post {
            val displayWidth = container.width.toFloat()
            val displayHeight = container.height.toFloat()

            if (displayWidth <= 0 || displayHeight <= 0) return@post

            val scaleX = displayWidth / layout.width.toFloat()
            val scaleY = displayHeight / layout.height.toFloat()

            var maxRegionDuration = 10

            for (region in layout.regions) {
                val regionView = FrameLayout(context)
                val regionWidth = (region.width * scaleX).toInt()
                val regionHeight = (region.height * scaleY).toInt()
                val regionLeft = (region.left * scaleX).toInt()
                val regionTop = (region.top * scaleY).toInt()

                val params = FrameLayout.LayoutParams(regionWidth, regionHeight).apply {
                    leftMargin = regionLeft
                    topMargin = regionTop
                }
                container.addView(regionView, params)

                val regionDuration = startRegionPlayback(region, regionView, layout.id, scheduleId)
                if (regionDuration > maxRegionDuration) {
                    maxRegionDuration = regionDuration
                }
            }

            // Schedule layout transition callback after max duration
            val cycleRunnable = Runnable {
                proofOfPlayEngine.onLayoutEnd()
                onLayoutCycleComplete?.invoke()
            }
            activeRunnables.add(cycleRunnable)
            handler.postDelayed(cycleRunnable, maxRegionDuration * 1000L)
        }
    }

    private fun startRegionPlayback(
        region: LayoutRegion,
        regionContainer: ViewGroup,
        layoutId: Long,
        scheduleId: Long
    ): Int {
        val mediaList = region.mediaItems
        if (mediaList.isEmpty()) return 10

        var totalDuration = 0
        for (m in mediaList) {
            totalDuration += m.durationSeconds
        }
        if (totalDuration <= 0) totalDuration = 10

        renderMediaItem(mediaList, 0, regionContainer, layoutId, scheduleId)
        return totalDuration
    }

    private fun renderMediaItem(
        mediaList: List<MediaItem>,
        index: Int,
        container: ViewGroup,
        layoutId: Long,
        scheduleId: Long
    ) {
        val media = mediaList[index % mediaList.size]
        container.removeAllViews()

        val file = storageManager.getFile(media.uri)
        proofOfPlayEngine.recordMediaPlayback(layoutId, scheduleId, media.id, media.durationSeconds.toLong())

        when (media.type) {
            MediaType.VIDEO -> {
                val videoRenderer = VideoRegionRenderer(context)
                activeVideoRenderers.add(videoRenderer)
                videoRenderer.attachTo(container)
                videoRenderer.playVideo(file, loop = mediaList.size == 1)
            }
            MediaType.IMAGE -> {
                val imgRenderer = ImageRegionRenderer(context)
                imgRenderer.attachTo(container)
                imgRenderer.displayImage(file, media.options["scaleType"] ?: "fit")
            }
            MediaType.WEBPAGE -> {
                val webRenderer = WebWidgetRenderer(context)
                activeWebRenderers.add(webRenderer)
                webRenderer.attachTo(container)
                if (media.uri.startsWith("http://") || media.uri.startsWith("https://")) {
                    webRenderer.loadUrl(media.uri)
                } else if (file.exists()) {
                    webRenderer.loadUrl("file://${file.absolutePath}")
                }
            }
            MediaType.TEXT, MediaType.TICKER, MediaType.CLOCK -> {
                val webRenderer = WebWidgetRenderer(context)
                activeWebRenderers.add(webRenderer)
                webRenderer.attachTo(container)
                val html = if (media.rawContent.isNotBlank()) media.rawContent else "<h2>${media.options["text"] ?: ""}</h2>"
                webRenderer.loadHtml(html)
            }
            MediaType.UNKNOWN -> {
                SystemLogger.w(TAG, "Unknown media type for media id=${media.id}")
            }
        }

        // If there are multiple items in region, schedule transition to next item
        if (mediaList.size > 1) {
            val nextRunnable = Runnable {
                renderMediaItem(mediaList, index + 1, container, layoutId, scheduleId)
            }
            activeRunnables.add(nextRunnable)
            handler.postDelayed(nextRunnable, media.durationSeconds * 1000L)
        }
    }

    fun clear(container: ViewGroup) {
        for (r in activeRunnables) {
            handler.removeCallbacks(r)
        }
        activeRunnables.clear()

        for (vr in activeVideoRenderers) {
            vr.release()
        }
        activeVideoRenderers.clear()

        for (wr in activeWebRenderers) {
            wr.release()
        }
        activeWebRenderers.clear()

        container.removeAllViews()
    }

    companion object {
        private const val TAG = "LayoutRenderer"
    }
}
