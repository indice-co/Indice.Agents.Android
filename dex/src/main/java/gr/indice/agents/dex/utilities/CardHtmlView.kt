package gr.indice.agents.dex.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.ceil


@Composable
fun ChatHtmlView(html: String, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val uriHandler = LocalUriHandler.current
    var contentHeight by remember { mutableFloatStateOf(1f) }
    val document = remember(html, isDark) { htmlDocument(html, isDark) }
    val controller = remember { HtmlController() }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(contentHeight.dp),
        factory = { context -> controller.createWebView(context) },
        update = { view ->
            controller.onHeight = { contentHeight = it }
            controller.openLink = { uriHandler.openUri(it) }
            controller.currentHeight = { contentHeight }
            if (controller.document != document) {
                controller.document = document
                controller.generation++
                view.loadDataWithBaseURL(null, document, "text/html", "utf-8", null)
            }
        },
        onRelease = { controller.release(it) },
    )
}

private class ContentWebView(context: Context) : WebView(context) {
    var onWidthChange: (() -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) onWidthChange?.invoke()
    }
}

private class HtmlController {
    var onHeight: (Float) -> Unit = {}
    var openLink: (String) -> Unit = {}
    var currentHeight: () -> Float = { 1f }
    var document: String? = null
    var generation = 0
    private var measuring = false

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(context: Context): ContentWebView = ContentWebView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = WebView.OVER_SCROLL_NEVER

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            cacheMode = WebSettings.LOAD_NO_CACHE
            textZoom = 100
            setSupportZoom(false)
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) = measureWithRetries(view as ContentWebView)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (request.hasGesture() && url.scheme?.lowercase() in setOf("http", "https")) {
                    openLink(url.toString())
                }
                return true
            }
        }
        onWidthChange = { measure(this) }
    }

    // Images can finish loading after onPageFinished and change the height.
    private fun measureWithRetries(view: ContentWebView) {
        measure(view)
        view.postDelayed({ measure(view) }, 300)
        view.postDelayed({ measure(view) }, 1_500)
    }

    fun measure(view: WebView) {
        if (measuring || view.progress < 100 || view.width <= 0) return
        measuring = true
        val requestedGeneration = generation
        // Fixed, app-owned expression, never code from a response. Measure the
        // content element (not the document height) so a tall part can shrink again.
        view.evaluateJavascript(
            "document.getElementById('indice-content').getBoundingClientRect().height"
        ) { result ->
            measuring = false
            if (requestedGeneration != generation) return@evaluateJavascript
            val raw = result?.toFloatOrNull() ?: return@evaluateJavascript
            val measured = ceil(raw).coerceIn(1f, 20_000f)
            if (abs(currentHeight() - measured) > 0.5f) onHeight(measured)
        }
    }

    fun release(view: ContentWebView) {
        view.onWidthChange = null
        view.removeCallbacks(null)
        view.stopLoading()
        view.webViewClient = WebViewClient()
        view.destroy()
    }
}

private fun htmlDocument(html: String, isDark: Boolean): String {
    val scheme = if (isDark) "dark" else "light"
    val textColor = if (isDark) "#f5f5f5" else "#202124"
    val linkColor = if (isDark) "#80b5ff" else "#1263c5"
    return """
        <!doctype html><html><head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'none'; style-src 'unsafe-inline'; img-src data: https: http:; frame-src 'none'; object-src 'none'; connect-src 'none'; form-action 'none'; base-uri 'none'">
        <style>
        :root { color-scheme: $scheme; }
        html, body { margin: 0; padding: 0; background: transparent; }
        body { font: 17px sans-serif; color: $textColor; line-height: 1.5; overflow-wrap: anywhere; }
        #indice-content { display: flow-root; padding: 2px; }
        * { box-sizing: border-box; }
        img, svg { max-width: 100%; height: auto; }
        p { margin: .35em 0; }
        h1, h2, h3, h4 { margin: .6em 0 .2em; line-height: 1.2; }
        h1 { font-size: 1.5em; } h2 { font-size: 1.3em; } h3 { font-size: 1.1em; }
        a { color: $linkColor; }
        pre { white-space: pre-wrap; } table { border-collapse: collapse; max-width: 100%; }
        th, td { padding: .3em; border: 1px solid #8886; }
        figure { margin: 0; }
        .dex-card { display: flex; gap: 1em; align-items: flex-start; padding: 1em; border: 1px solid #8886; border-radius: 1em; }
        .dex-card img { width: 5em; height: 5em; border-radius: 50%; object-fit: cover; flex: none; }
        .dex-muted { opacity: .7; font-size: .85em; }
        .dex-badge { display: inline-block; padding: .1em .6em; border-radius: 1em; font-size: .8em; background: #8883; }
        </style></head><body><div id="indice-content">$html</div></body></html>
    """.trimIndent()
}