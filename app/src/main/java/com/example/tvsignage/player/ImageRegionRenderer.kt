package com.example.tvsignage.player

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import coil.load
import java.io.File

class ImageRegionRenderer(context: Context) {

    val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    fun attachTo(container: ViewGroup) {
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(imageView, params)
    }

    fun displayImage(file: File, scaleMode: String = "fit") {
        when (scaleMode.lowercase()) {
            "stretch" -> imageView.scaleType = ImageView.ScaleType.FIT_XY
            "crop", "fill" -> imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            else -> imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        imageView.load(file) {
            crossfade(300)
        }
    }

    fun clear() {
        imageView.setImageDrawable(null)
    }
}
