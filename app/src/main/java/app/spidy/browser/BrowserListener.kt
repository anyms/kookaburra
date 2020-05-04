package app.spidy.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Handler
import android.os.Message
import android.util.Log
import android.webkit.*
import androidx.fragment.app.FragmentActivity
import app.spidy.hiper.Hiper
import app.spidy.hiper.data.HiperResponse
import app.spidy.kookaburra.controllers.Browser
import kotlin.collections.HashMap

class BrowserListener : Browser.Listener {
    override fun onLoadResource(view: WebView, url: String) {}

    override fun onPageStarted(view: WebView, url: String, favIcon: Bitmap?) {}

    override fun onPageFinished(view: WebView, url: String) {}

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) {}

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError?
    ) {}

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {}

    override fun shouldInterceptRequest(
        view: WebView,
        activity: FragmentActivity?,
        url: String,
        request: WebResourceRequest?
    ) {}

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {}

    override fun onPageCommitVisible(view: WebView, url: String) {}

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {}

    override fun onNewUrl(view: WebView, url: String) {}

    override fun onNewDownload(
        view: WebView,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String,
        contentLength: Long
    ) {}

    override fun onSwitchTab(fromTabId: String, toTabId: String) {}

    override fun onNewTab(tabId: String) {}

    override fun onCloseTab(tabId: String) {}

    override fun onRestoreTab(tabId: String, isActive: Boolean) {}
}