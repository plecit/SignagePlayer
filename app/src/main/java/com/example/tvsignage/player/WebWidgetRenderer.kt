package com.example.tvsignage.player

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.example.tvsignage.core.SystemLogger

class WebWidgetRenderer(context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        setBackgroundColor(0) // Transparent background
        webViewClient = object : WebViewClient() {}
        webChromeClient = WebChromeClient()
    }

    fun attachTo(container: ViewGroup) {
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(webView, params)
    }

    fun loadHtml(htmlContent: String, baseUrl: String? = null) {
        val styledHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        overflow: hidden;
                        background-color: transparent;
                        color: #ffffff;
                        font-family: sans-serif;
                    }
                </style>
            </head>
            <body>
                $htmlContent
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(baseUrl, styledHtml, "text/html", "UTF-8", null)
    }

    fun loadUrl(url: String) {
        SystemLogger.i(TAG, "Loading web URL: $url")
        webView.loadUrl(url)
    }

    fun release() {
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Error destroying WebView: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "WebWidgetRenderer"
    }
}
