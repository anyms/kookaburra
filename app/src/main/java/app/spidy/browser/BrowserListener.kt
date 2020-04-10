package app.spidy.browser

import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import app.spidy.hiper.Hiper
import app.spidy.hiper.data.HiperResponse
import app.spidy.kookaburra.controllers.Browser
import kotlin.collections.HashMap

class BrowserListener : Browser.Listener {
    private var hiper = Hiper.getAsyncInstance()
    private val cookieManager = CookieManager.getInstance()
    private var cookies = HashMap<String, String>()

    override fun shouldInterceptRequest(view: WebView, url: String, request: WebResourceRequest?) {
        init(url, request?.requestHeaders, view)
    }

    override fun onNewUrl(url: String) {
        cookies = HashMap()
        val cooks = cookieManager.getCookie(url)?.split(";")
        if (cooks != null) {
            for (cook in cooks) {
                val nodes = cook.trim().split("=")
                cookies[nodes[0].trim()] = nodes[1].trim()
            }
        }
    }

    private fun init(url: String, headers: Map<String, String>?, view: WebView) {
        val heads = (headers as? HashMap<String, Any>) ?: hashMapOf()
        hiper.head(url, headers = heads, cookies = cookies).then { response ->
            checkResponse(url, response, view)
        }.catch { e ->
            Log.d("hello", "Err: $e")
        }
    }

    private fun checkResponse(url: String, response: HiperResponse, webView: WebView) {
        val contentLength = response.headers.get("content-length")?.toLong()
        val contentType = response.headers.get("content-type")
        if (contentType != null && contentLength != null && contentLength > 0 && (
                contentType.startsWith("video/") || contentType.startsWith("audio/")
            )
        ) {
            Log.d("hello", contentLength.toString())
            Log.d("hello", url)
        }
    }
}