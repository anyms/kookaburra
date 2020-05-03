package app.spidy.kookaburra.controllers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Message
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import app.spidy.kookaburra.data.History
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.concurrent.thread

class ChromeClient(
    private val context: Context,
    private val browser: Browser
) : WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var originalOrientation: Int? = null
    private var originalSystemUiVisibility: Int? = null

    override fun onReceivedTitle(view: WebView?, title: String?) {
        title?.also {
            if (view != null) browser.setTitle(view, title)
            browser.tabAdapter.notifyDataSetChanged()

            thread {
                val lastHistory = browser.lastHistory
                if (browser.url != lastHistory?.url.toString()) {
                    browser.putHistory(
                        History(
                            title,
                            browser.url,
                            getCurrentDateTime().toString("yyyy/MM/dd HH:mm:ss")
                        )
                    )
                }
            }
        }
        super.onReceivedTitle(view, title)
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        if (browser.progressBar.visibility == View.GONE) {
            browser.progressBar.visibility = View.VISIBLE
        }
        browser.progress = newProgress
        super.onProgressChanged(view, newProgress)
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        if (browser.currentTab?.favIcon == null) {
            icon?.also {
                val stream = ByteArrayOutputStream()
                icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
                browser.currentTab?.favIcon = stream.toByteArray()
                browser.tabAdapter.notifyDataSetChanged()
            }
        }

        super.onReceivedIcon(view, icon)
    }

    /* Allow videos to go fullscreen */
    override fun getDefaultVideoPoster(): Bitmap? {
        if (this.customView == null) {
            return null
        }
        return BitmapFactory.decodeResource(context.resources, 2130837573)
    }

    override fun onHideCustomView() {
        ((context as AppCompatActivity).window.decorView as FrameLayout).removeView(this.customView)
        this.customView = null
        context.window.decorView.systemUiVisibility = this.originalSystemUiVisibility!!
        context.requestedOrientation = this.originalOrientation!!
        this.customViewCallback?.onCustomViewHidden()
        this.customViewCallback = null
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (this.customView != null) {
            onHideCustomView()
            return
        }

        this.customView = view
        this.customView?.setBackgroundColor(Color.BLACK)
        this.originalSystemUiVisibility = (context as AppCompatActivity).window.decorView.systemUiVisibility
        this.originalOrientation = context.requestedOrientation
        this.customViewCallback = callback
        (context.window.decorView as FrameLayout).addView(this.customView, FrameLayout.LayoutParams(-1, -1))
        context.window.decorView.systemUiVisibility = 3846
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val href = view?.handler?.obtainMessage()
        view?.requestFocusNodeHref(href)
        val url = href?.data?.getString("url")

        url?.also {
            browser.newTab(it)
        }

        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }
}