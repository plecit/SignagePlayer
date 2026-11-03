package com.example.tvsignage.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.tvsignage.core.SystemLogger
import java.io.File

/**
 * Handles seamless video playback inside a region using AndroidX Media3 ExoPlayer.
 * Supports dual player instances for zero-gap switching between consecutive videos.
 */
@OptIn(UnstableApi::class)
class VideoRegionRenderer(private val context: Context) {

    private val player1 = ExoPlayer.Builder(context).build()
    private val player2 = ExoPlayer.Builder(context).build()

    private val playerView1 = PlayerView(context).apply {
        player = player1
        useController = false
    }
    private val playerView2 = PlayerView(context).apply {
        player = player2
        useController = false
        alpha = 0f
    }

    private var activePlayerIndex = 1

    fun attachTo(container: ViewGroup) {
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(playerView1, params)
        container.addView(playerView2, params)
    }

    fun playVideo(file: File, loop: Boolean = true, onEnded: (() -> Unit)? = null) {
        if (!file.exists()) {
            SystemLogger.e(TAG, "Video file does not exist: ${file.absolutePath}")
            onEnded?.invoke()
            return
        }

        val mediaItem = Media3Item.fromUri(Uri.fromFile(file))

        if (activePlayerIndex == 1) {
            // Switch to player 2
            player2.setMediaItem(mediaItem)
            player2.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            player2.prepare()
            player2.playWhenReady = true

            player2.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        playerView2.alpha = 1f
                        playerView1.alpha = 0f
                        player1.stop()
                    } else if (playbackState == Player.STATE_ENDED) {
                        onEnded?.invoke()
                    }
                }
            })
            activePlayerIndex = 2
        } else {
            // Switch to player 1
            player1.setMediaItem(mediaItem)
            player1.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            player1.prepare()
            player1.playWhenReady = true

            player1.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        playerView1.alpha = 1f
                        playerView2.alpha = 0f
                        player2.stop()
                    } else if (playbackState == Player.STATE_ENDED) {
                        onEnded?.invoke()
                    }
                }
            })
            activePlayerIndex = 1
        }
    }

    fun release() {
        player1.release()
        player2.release()
    }

    companion object {
        private const val TAG = "VideoRegionRenderer"
    }
}
