# A Light Weight Browser for Android


There are tons of Android browsers on GitHub, but none of them are supporting current API level. Android Q depricated lot of APIs so, I made this browser to be follow the Android Q supporting.

# Getting Started

To add the browser to your app, create an activity for example `BrowserActivity.kt`, then add the `BrowserFragment`.

```kotlin
import app.spidy.kookaburra.fragments.BrowserFragment


class BrowserActivity : AppCompatActivity() {
    private lateinit var browserFragment: BrowserFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        browserFragment = BrowserFragment()
        browserFragment.browserListener = BrowserListener()
        supportFragmentManager.beginTransaction()
            .add(R.id.browser_holder, browserFragment)
            .commit()
    }

    override fun onBackPressed() {
        if (!browserFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
```

The `BrowserListener` will gives you access to the all `webview` level listeners.

```kotlin
import app.spidy.kookaburra.controllers.Browser

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
```
