package app.spidy.kookaburra.controllers

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
import android.os.Parcel
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.room.Room
import app.spidy.kookaburra.R
import app.spidy.kookaburra.adapters.TabAdapter
import app.spidy.kookaburra.data.Bookmark
import app.spidy.kookaburra.data.History
import app.spidy.kookaburra.data.Tab
import app.spidy.kookaburra.databases.BrowserDatabase
import app.spidy.kookaburra.fragments.WebviewFragment
import app.spidy.kookaburra.interfaces.WebviewFragmentListener
import app.spidy.kookaburra.widgets.KookaburraWebView
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import com.google.android.material.appbar.AppBarLayout
import java.lang.Exception
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class Browser(
    private val context: Context,
    private val activity: AppCompatActivity,
    private val toolbar: Toolbar,
    private val appBarLayout: AppBarLayout,
    private val webviewHolder: FrameLayout,
    private val titleBar: TextView,
    private val urlField: EditText,
    val progressBar: ProgressBar,
    val browserListener: Listener?
) {


    interface Listener {
        fun onLoadResource(view: WebView, url: String) {}
        fun onPageStarted(view: WebView, url: String, favIcon: Bitmap?) {}
        fun onPageFinished(view: WebView, url: String) {}
        fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) {}
        fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError?) {}
        fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {}
        fun shouldInterceptRequest(view: WebView, url: String, request: WebResourceRequest?) {}
        fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {}
        fun onPageCommitVisible(view: WebView, url: String) {}
        fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {}
        fun onNewUrl(url: String) {}
    }


    companion object {
        const val KEY_LAST_TAB = "app.spidy.browser.LAST_TAB"

        val LAST_UPDATED = "1/27/2020"
    }

    lateinit var tabAdapter: TabAdapter

    val tabs = ArrayList<Tab>()
    var needle = 0
    var isLoading = false
    val sslErroredDomains = ArrayList<String>()

    var menuRefreshImage: ImageView? = null
    var protocolImage: ImageView? = null

    private val fileIO = FileIO(context)
    private val database = Room.databaseBuilder(context, BrowserDatabase::class.java, "BrowserDatabase")
        .fallbackToDestructiveMigration().build()
    private val tinyDB = TinyDB(context)

    val currentTab: Tab?
        get() {
            return try {
                tabs[needle]
            } catch (e: Exception) {
                null
            }
        }
    var tabCount: Int
        get() = tabs.size
        set(value) {
            if (value <= 9) {
                try {
                    val id = context.resources.getIdentifier("tab_count_${value}", "drawable", context.packageName)
                    toolbar.setNavigationIcon(id)
                } catch (e: Exception) {}
            } else {
                toolbar.setNavigationIcon(R.drawable.tab_count_9_plus)
            }
        }
    var url: String
        get() = tabs[needle].url
        set(value) {
            urlField.setText(value)
            tabs[needle].url = value
        }
    var title: String
        get() = tabs[needle].title
        set(value) {
            titleBar.text = value
            tabs[needle].title = value
        }
    val searchEngine: String
        get() {
            val engine = tinyDB.getString("search_engine")
            return if (engine == "" || engine == null) "https://duckduckgo.com/?q=" else engine
        }
    var progress: Int
        get() = progressBar.progress
        set(value) {
            progressBar.progress = value
        }

    val bookmarks: List<Bookmark>
        get() = database.browserDao().getBookmarks()

    val histories: List<History>
        get() = database.browserDao().getHistories(10000)

    val lastHistory: History?
        get() {
            val hs = database.browserDao().getHistories(1)
            return if (hs.isEmpty()) null else hs[0]
        }

    val isJavaScriptEnabled: Boolean
        get() = !tinyDB.getBoolean("disable_javascript")

    val isAdblockEnabled: Boolean
        get() = !tinyDB.getBoolean("disable_adblock")


    /* Listeners */

    private val webviewFragmentListener = object : WebviewFragmentListener {
        override fun onLoad(webview: KookaburraWebView) {
            webview.setActionBar(appBarLayout, webviewHolder)
        }
    }


    /* Public methods */

    fun newTab(url: String) {
        val fragment = WebviewFragment()
        fragment.urlToLoad = url
        fragment.browser = this
        thread {
            var needleTmp = 0
            val savedTabs = database.browserDao().getTabs()
            savedTabs.sortedBy { it.tabId }
            if (savedTabs.isNotEmpty()) {
                needleTmp = savedTabs.size
            }
            fragment.webviewId = UUID.randomUUID().toString()

            onUiThread {
                fragment.webviewFragmentListener = webviewFragmentListener

                val transaction = activity.supportFragmentManager.beginTransaction()
                if (tabs.size == 0) {
                    transaction.add(R.id.webview_holder, fragment)
                } else {
                    if (tabs[needle].fragment!!.isAdded) {
                        tabs[needle].fragment!!.webview.onPause()
                    }

                    transaction.hide(tabs[needle].fragment!!)
                        .add(R.id.webview_holder, fragment)
                }
                transaction.commit()
                val tab = Tab(context.getString(R.string.three_dots),
                    context.getString(R.string.three_dots), fragment.webviewId!!, fragment)
                thread {
                    database.browserDao().putTab(tab)
                }
                tabs.add(tab)
                tabCount = tabs.size
                needle = needleTmp

                tinyDB.putString(KEY_LAST_TAB, tab.tabId)

                titleBar.text = tab.title
            }
        }
    }

    fun switchTab(fromTab: Tab, toTab: Tab, defaultNeedle: Int? = null) {
        fromTab.fragment!!.webview.onPause()
        val transaction = activity.supportFragmentManager.beginTransaction()
            .hide(fromTab.fragment!!)

        if (!toTab.fragment!!.isAdded) {
            transaction.add(R.id.webview_holder, toTab.fragment!!)
        } else {
            transaction.show(toTab.fragment!!)
            toTab.fragment!!.webview.onResume()
        }
        transaction.commit()
        needle = defaultNeedle ?: findTabIndex(toTab)
        progressBar.visibility = View.GONE
        urlField.setText(toTab.url)
        titleBar.text = toTab.title

        tinyDB.putString(KEY_LAST_TAB, toTab.tabId)
    }

    private fun findTabIndex(tab: Tab): Int {
        var index: Int? = null
        for (i in tabs.indices) {
            if (tabs[i].tabId == tab.tabId) {
                index = i
            }
        }
        return index!!
    }

    fun closeTab(tab: Tab) {
        thread {
            database.browserDao().removeTab(tab)

            onUiThread {
                fileIO.deleteFile("__data__", "cache_${tab.tabId}")
                val tabIndex = findTabIndex(tab)
                val cTab = currentTab
                tabs.remove(tab)
                if (tabs.isEmpty()) {
                    activity.finish()
                } else if (needle == tabIndex) {
                    switchTab(tab, tabs.last(), tabs.lastIndex)
                } else {
                    cTab?.also {
                        needle = findTabIndex(it)
                    }
                }
                tabCount = tabs.size
            }
        }
    }

    private fun restoreTab(tab: Tab, canAdd: Boolean) {
        tab.fragment = WebviewFragment()
        tab.fragment!!.urlToLoad = tab.url
        tab.fragment!!.browser = this
        tab.fragment!!.webviewId = tab.tabId
        tab.fragment!!.webviewFragmentListener = webviewFragmentListener
        tabs.add(tab)

        if (canAdd) {
            (activity as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                    ?.add(R.id.webview_holder, tab.fragment!!)
                    ?.commit()
            needle = findTabIndex(tab)
        }
    }

    fun restoreTabs() {
        thread {
            val savedTabs = database.browserDao().getTabs()
            onUiThread {
                for (tab in savedTabs) {
                    if (tab.tabId == tinyDB.getString(KEY_LAST_TAB)) {
                        ignore { restoreTab(tab, true) }
                    } else {
                        restoreTab(tab, false)
                    }
                    tabCount = tabs.size
                }

                if (savedTabs.isEmpty()) {
                    newTab("https://${URI(searchEngine).host}")
                }
            }
        }
    }

    fun updateTab(tab: Tab) {
        thread {
            database.browserDao().updateTab(tab)
        }
    }

    fun removeBookmark(bookmark: Bookmark) {
        database.browserDao().removeBookmark(bookmark)
    }

    fun putBookmark(bookmark: Bookmark) {
        database.browserDao().putBookmark(bookmark)
    }

    fun clearAllBookmarks() {
        database.browserDao().clearAllBookmarks()
    }

    fun removeHistory(history: History) {
        database.browserDao().removeHistory(history)
    }

    fun putHistory(history: History) {
        database.browserDao().putHistory(history)
    }

    fun clearAllHistory() {
        database.browserDao().clearAllHistory()
    }

    fun browse(q: String) {
        if (q.startsWith("http://") || q.startsWith("https://")) {
            tabs[needle].fragment!!.webview.loadUrl(q)
        } else if (
            q.contains("[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}".toRegex()) ||
            q.contains("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|\$)){4}\\b".toRegex()) ||
            q.startsWith("localhost")
        ) {
            currentTab?.fragment?.webview?.loadUrl("http://$q")
        } else {
            currentTab?.fragment?.webview?.loadUrl("$searchEngine$q")
        }
    }

    fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    fun saveState(tabId: String, webView: WebView) {
        var parcel: Parcel? = null
        try {
            val bundle = Bundle()
            webView.saveState(bundle)
            parcel = Parcel.obtain()
            bundle.writeToParcel(parcel, 0)
            fileIO.saveBytes("__data__", "cache_$tabId", parcel.marshall())
        } catch (e: Exception) {
            context.toast("Unable to save state")
        } finally {
            parcel?.recycle()
        }
    }

    fun loadState(tabId: String, webView: WebView, onException: () -> Unit) {
        var parcel: Parcel? = null
        try {
            val data = fileIO.readBytes("__data__", "cache_$tabId")
            parcel = Parcel.obtain()
            parcel.unmarshall(data, 0, data.size)
            parcel.setDataPosition(0)
            val out = parcel.readBundle(ClassLoader.getSystemClassLoader())
            out?.putAll(out)
            webView.restoreState(out)
        } catch (e: Exception) {
            onException()
        } finally {
            parcel?.recycle()
        }
    }

    /* Private methods */


}