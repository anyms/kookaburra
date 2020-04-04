package app.spidy.kookaburra.controllers

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import app.spidy.kookaburra.R
import app.spidy.kookaburra.data.History
import app.spidy.kotlinutils.toast
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.concurrent.thread


class WebClient(
    private val context: Context,
    private val browser: Browser
) : WebViewClient() {
    private var lastUrl: String? = null

    override fun onLoadResource(view: WebView?, url: String?) {
        if (lastUrl == null || !view?.url.equals(lastUrl)) {
            if (view?.url != null) {
                val prettyUrl = if (view.url.endsWith('/')) view.url.trimEnd('/') else view.url
                lastUrl = view.url
                browser.url = prettyUrl
                browser.tabAdapter.notifyDataSetChanged()
                when {
                    browser.sslErroredDomains.contains(URI(url!!).host) -> {
                        browser.protocolImage?.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.protocol_error))
                    }
                    view.url.startsWith("https://") -> {
                        browser.protocolImage?.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.protocol_secure))
                    }
                    else -> {
                        browser.protocolImage?.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.protocol_info))
                    }
                }
            }
        }

        super.onLoadResource(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        if (!NetworkUtil.isNetworkAvailable(view?.context)) {
            view?.context?.toast("Network unavailable!", true)
        }
        browser.isLoading = true
        browser.currentTab?.favIcon = null
        browser.showProgressBar()
        view?.also {
            browser.saveState(browser.currentTab?.tabId.toString(), view)
            browser.menuRefreshImage?.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.ic_close))
        }
        if (browser.currentTab != null) {
            browser.updateTab(browser.currentTab!!)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.also {
            browser.saveState(browser.currentTab?.tabId.toString(), view)
            browser.menuRefreshImage?.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.refresh_icon))
        }
        if (browser.currentTab != null) {
            browser.updateTab(browser.currentTab!!)
        }
        browser.isLoading = false
        browser.hideProgressBar()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.also {
            if (it.url.toString().startsWith("intent://")) {
                try {
                    val context: Context = view!!.context
                    val intent = Intent.parseUri(it.url.toString(), Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        view.stopLoading()
                        val packageManager: PackageManager = context.getPackageManager()
                        val info = packageManager.resolveActivity(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        )
                        if (info != null) {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } else {
                            val fallbackUrl =
                                intent.getStringExtra("browser_fallback_url")
                            view.loadUrl(fallbackUrl)
                        }
                        return true
                    }
                } catch (e: URISyntaxException) {
                    Log.e("debug_exception", "Can't resolve intent://", e)
                }
            }
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view?.context?.toast("An error occurred : ${error?.errorCode}")
        }
        super.onReceivedError(view, request, error)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (view == null) {
            super.onReceivedSslError(view, handler, error)
            return
        }

        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setCancelable(false)
        alertDialog.setTitle(view.context.getString(R.string.ssl_error_title))
        alertDialog.setMessage(view.context.getString(R.string.ssl_error_message))
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, view.context.getString(R.string.ssl_error_back_btn)) { dialog, which ->
            browser.also {
                super.onReceivedSslError(view, handler, error)
            }
        }
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, view.context.getString(R.string.ssl_error_proceed_btn)) { dialog, which ->
            handler?.proceed()
            error?.url?.also {
                browser.sslErroredDomains.add(URI(it).host)
            }
        }
        alertDialog.show()
    }
}