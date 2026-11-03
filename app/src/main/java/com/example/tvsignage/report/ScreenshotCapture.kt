package com.example.tvsignage.report

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.PixelCopy
import android.view.View
import android.view.Window
import com.example.tvsignage.core.SystemLogger
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ScreenshotCapture {

    private const val TAG = "ScreenshotCapture"

    suspend fun captureWindowAsBase64Jpeg(activity: Activity, quality: Int = 75): String {
        val window = activity.window
        val view = window.decorView

        if (view.width <= 0 || view.height <= 0) return ""

        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureWithPixelCopy(window, view)
        } else {
            captureWithDrawingCache(view)
        } ?: return ""

        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val bytes = baos.toByteArray()
            bitmap.recycle()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to encode screenshot: ${e.message}", e)
            ""
        }
    }

    private suspend fun captureWithPixelCopy(window: Window, view: View): Bitmap? = suspendCoroutine { cont ->
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(
                window,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        cont.resume(bitmap)
                    } else {
                        SystemLogger.w(TAG, "PixelCopy returned error code: $copyResult, using fallback canvas")
                        cont.resume(captureWithDrawingCache(view))
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            SystemLogger.w(TAG, "PixelCopy exception: ${e.message}, using fallback canvas")
            cont.resume(captureWithDrawingCache(view))
        }
    }

    private fun captureWithDrawingCache(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Fallback canvas capture failed", e)
            null
        }
    }
}
