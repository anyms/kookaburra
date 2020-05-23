package app.spidy.kookaburra.fragments


import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.hiper.Hiper
import app.spidy.kookaburra.R
import app.spidy.kookaburra.adapters.SuggestionAdapter
import app.spidy.kookaburra.adapters.BookmarkAdapter
import app.spidy.kookaburra.adapters.HistoryAdapter
import app.spidy.kookaburra.adapters.TabAdapter
import app.spidy.kookaburra.controllers.Browser
import app.spidy.kookaburra.data.Bookmark
import app.spidy.kookaburra.controllers.PermissionHandler
import app.spidy.kookaburra.controllers.getCurrentDateTime
import app.spidy.kookaburra.controllers.toString
import app.spidy.kookaburra.data.History
import app.spidy.kookaburra.data.Tab
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import java.io.File
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread


class BrowserFragment : Fragment() {
    companion object {
        private const val REQUEST_CODE_SPEECH_INPUT = 1
    }

    private lateinit var toolbar: Toolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var webviewHolder: FrameLayout
    private lateinit var titleBar: TextView
    private lateinit var urlField: EditText
    private lateinit var urlBar: ConstraintLayout
    private lateinit var urlMicrophoneImage: ImageView
    private lateinit var browserOverlay: FrameLayout
    private lateinit var suggestionRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var clearUrlImage: ImageView
    private lateinit var tabsDialog: Dialog
    private lateinit var tabsDialogTabCountIcon: ImageView
    private lateinit var menuDialog: AlertDialog
    private lateinit var bookmarkDialog: Dialog
    private lateinit var historyDialog: Dialog
    private lateinit var settingsDialog: Dialog
    private lateinit var protocolDialog: AlertDialog
    private lateinit var protocolImage: ImageView
    private lateinit var protocolStatusView: TextView
    private lateinit var protocolDomainView: TextView
    private lateinit var protocolMessageView: TextView
    private lateinit var suggestionAdapter: SuggestionAdapter

    private lateinit var browser: Browser
    private lateinit var tabAdapter: TabAdapter

    private val viewGroup: ViewGroup? = null

    var browserListener: Browser.Listener? = null
    private var menu: Menu? = null
    private var isOverlayShowing = false
    private val suggestions = ArrayList<String>()
    private val hiper = Hiper.getAsyncInstance()
    private val searchEngineCookies = HashMap<String, String>()
    private val additionalMenuViews = ArrayList<View>()
    private val additionalMenuCallbacks = ArrayList<(View) -> Unit>()
    private val optionMenuCallbacks = HashMap<Int, () -> Unit>()


    val currentTab: Tab?
        get() = browser.currentTab


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.browser_fragment_browser, container, false)

        PermissionHandler.requestStorage(view.context, "To download files, the browser require storage permission. \n" +
                "\n" +
                " Would you like to grant?") {}


        toolbar = view.findViewById(R.id.browser_toolbar)
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        webviewHolder = view.findViewById(R.id.webview_holder)
        titleBar = view.findViewById(R.id.title_bar)
        urlField = view.findViewById(R.id.url_field)
        urlMicrophoneImage = view.findViewById(R.id.url_microphone)
        urlBar = view.findViewById(R.id.browser_urlbar)
        browserOverlay = view.findViewById(R.id.browser_overlay)
        suggestionRecyclerView = view.findViewById(R.id.suggestion_recyclerview)
        progressBar = view.findViewById(R.id.progressBar)
        clearUrlImage = view.findViewById(R.id.clear_url)
        protocolImage = view.findViewById(R.id.protocol_image)

        browser = Browser(
            requireContext(),
            (activity as AppCompatActivity),
            toolbar,
            appBarLayout,
            webviewHolder,
            titleBar,
            urlField,
            progressBar,
            browserListener
        )
        suggestionAdapter = SuggestionAdapter(requireContext(), suggestions, browser, urlField) {
            hideUrlField()
        }
        tabsDialog = createTabDialog()
        menuDialog = createOptionMenu(requireContext())
        protocolDialog = createProtocolDialog(requireContext())
        bookmarkDialog = createBookmarkDialog()
        historyDialog = createHistoryDialog()
        settingsDialog = createSettingsDialog()
        browser.tabAdapter = tabAdapter
        browser.protocolImage = protocolImage

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        suggestionRecyclerView.adapter = suggestionAdapter
        suggestionRecyclerView.layoutManager = LinearLayoutManager(context)

        tabsDialog.setOnShowListener {
            tabAdapter.notifyDataSetChanged()
            if (browser.tabs.size <= 9) {
                ignore {
                    context?.resources?.getIdentifier("browser_tab_count_${browser.tabs.size}",
                        "drawable", context?.packageName)?.also {
                        tabsDialogTabCountIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), it))
                    }
                }
            } else {
                tabsDialogTabCountIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.browser_tab_count_9_plus))
            }
        }

        toolbar.setNavigationOnClickListener {
            tabsDialog.show()
        }
        urlMicrophoneImage.setOnClickListener {
            speak()
        }
        toolbar.setOnClickListener {
            toolbar.visibility = View.GONE
            urlBar.visibility = View.VISIBLE
            browserOverlay.visibility = View.VISIBLE
            suggestionRecyclerView.visibility = View.VISIBLE
            urlField.requestFocus()

            // Select text from search bar
            urlField.setSelectAllOnFocus(true)
            urlField.selectAll()

            // Show keyboard
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(urlField, InputMethodManager.SHOW_IMPLICIT)

            isOverlayShowing = true
        }

        browserOverlay.setOnClickListener {
            hideUrlField()
        }

        /* Detect search key and load webView with given url */
        urlField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = urlField.text.toString()
                browser.browse(q)
                hideUrlField()
            }

            return@setOnEditorActionListener true
        }

        val cooks = browser.cookieManager.getCookie("https://www.google.com")?.split(";")
        if (cooks != null) {
            for (cook in cooks) {
                val nodes = cook.trim().split("=")
                searchEngineCookies[nodes[0].trim()] = nodes[1].trim()
            }
        }

        urlField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()
                suggestions.clear()
                if (query == null || query == "") {
                    suggestionAdapter.notifyDataSetChanged()
                } else {
                    hiper.get("https://suggestqueries.google.com/complete/search?client=firefox&q=$query", cookies = searchEngineCookies)
                        .then {
                            val arr = JSONArray(it.text)
                            if (arr.length() >= 2) {
                                val suggs = arr.getJSONArray(1)
                                for (i in 0 until suggs.length()) {
                                    suggestions.add(suggs.getString(i))
                                }
                            }
                            onUiThread {
                                suggestionAdapter.notifyDataSetChanged()
                            }
                            it.close()
                        }.catch {
                            onUiThread {
                                suggestionAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
        })

        clearUrlImage.setOnClickListener {
            urlField.setText("")
        }

        browser.restoreTabs()

        return view
    }

    private fun hideUrlField() {
        urlField.clearFocus()
        toolbar.visibility = View.VISIBLE
        urlBar.visibility = View.GONE
        browserOverlay.visibility = View.GONE
        suggestionRecyclerView.visibility = View.GONE

        // Hide keyboard
        val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        isOverlayShowing = false
    }

    private fun speak() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Try saying something")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            context?.toast("Something went wrong!")
        }
    }

    private fun createSettingsDialog(): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialogTheme)
        val view = layoutInflater.inflate(R.layout.browser_layout_settings_dialog, viewGroup)
        val closeImage: ImageView = view.findViewById(R.id.settings_close_image)

        closeImage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.window?.also {
            it.attributes.windowAnimations = R.style.SlideUpAndDownAnimationTheme

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.statusBarColor = Color.BLACK
            }
        }

        dialog.setOnDismissListener {
            browser.currentTab?.fragment?.webview?.settings?.javaScriptEnabled = browser.isJavaScriptEnabled
        }

        return dialog
    }

    private fun createProtocolDialog(context: Context): AlertDialog {
        val builder = AlertDialog.Builder(context, R.style.BrowserTheme_DialogTheme)
        val protocolDialogView = LayoutInflater.from(context).inflate(R.layout.browser_layout_protocol_message, viewGroup, false)
        builder.setView(protocolDialogView)
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val wind = dialog.window
        wind?.setBackgroundDrawableResource(android.R.color.transparent)
        val params = wind?.attributes
        params?.gravity = Gravity.TOP or Gravity.CENTER
        wind?.attributes = params


        protocolStatusView = protocolDialogView.findViewById(R.id.statusView)
        protocolDomainView = protocolDialogView.findViewById(R.id.domainView)
        protocolMessageView = protocolDialogView.findViewById(R.id.messageView)

        return dialog
    }

    private fun createBookmarkDialog(): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialogTheme)
        val view = layoutInflater.inflate(R.layout.browser_layout_bookmarks_dialog, viewGroup)
        val bookmarkRecyclerView: RecyclerView = view.findViewById(R.id.bookmarks_recyclerview)
        val bookmarkCloseImage: ImageView = view.findViewById(R.id.bookmarks_close_image)
        val bookmarkMenuImage: ImageView = view.findViewById(R.id.bookmarks_menu_image)
        val bookmarks = ArrayList<Bookmark>()
        val bookmarkAdapter = BookmarkAdapter(requireContext(), bookmarks, dialog, browser)
        bookmarkRecyclerView.adapter = bookmarkAdapter
        bookmarkRecyclerView.layoutManager = LinearLayoutManager(context)

        dialog.setOnShowListener {
            thread {
                bookmarks.clear()
                browser.bookmarks.forEach {
                    bookmarks.add(it)
                }
                bookmarks.reverse()
                onUiThread {
                    bookmarkAdapter.notifyDataSetChanged()
                }
            }
        }

        bookmarkMenuImage.setOnClickListener {
            val popupMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                PopupMenu(context, bookmarkMenuImage, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)
            } else {
                PopupMenu(context, bookmarkMenuImage)
            }
            popupMenu.inflate(R.menu.browser_menu_simple)
            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.remove_all -> {
                        thread {
                            browser.clearAllBookmarks()
                        }
                        context?.toast("All bookmarks are deleted")
                        dialog.dismiss()
                    }
                    R.id.close -> {
                        dialog.dismiss()
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        bookmarkCloseImage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.window?.also {
            it.attributes.windowAnimations = R.style.SlideUpAndDownAnimationTheme

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.statusBarColor = Color.BLACK
            }
        }

        return dialog
    }


    private fun createHistoryDialog(): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialogTheme)
        val view = layoutInflater.inflate(R.layout.browser_layout_history_dialog, viewGroup)
        val historyRecyclerView: RecyclerView = view.findViewById(R.id.history_recyclerview)
        val historyCloseImage: ImageView = view.findViewById(R.id.history_close_image)
        val historyMenuImage: ImageView = view.findViewById(R.id.history_menu_image)
        val histories = ArrayList<History>()
        val historyAdapter = HistoryAdapter(requireContext(), histories, dialog, browser)
        historyRecyclerView.adapter = historyAdapter
        historyRecyclerView.layoutManager = LinearLayoutManager(context)

        dialog.setOnShowListener {
            thread {
                histories.clear()
                browser.histories.forEach {
                    histories.add(it)
                }
                onUiThread {
                    historyAdapter.notifyDataSetChanged()
                }
            }
        }

        historyMenuImage.setOnClickListener {
            val popupMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                PopupMenu(context, historyMenuImage, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)
            } else {
                PopupMenu(context, historyMenuImage)
            }
            popupMenu.inflate(R.menu.browser_menu_simple)
            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.remove_all -> {
                        thread {
                            browser.clearAllHistory()
                        }
                        context?.toast("History cleared")
                        dialog.dismiss()
                    }
                    R.id.close -> {
                        dialog.dismiss()
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        historyCloseImage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.window?.also {
            it.attributes.windowAnimations = R.style.SlideUpAndDownAnimationTheme

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.statusBarColor = Color.BLACK
            }
        }

        return dialog
    }


    private fun createTabDialog(): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialogTheme)
        val view = layoutInflater.inflate(R.layout.browser_layout_tabs_dialog, viewGroup)
        val tabsRecyclerView: RecyclerView = view.findViewById(R.id.bookmarks_recyclerview)
        val newTabIcon: ImageView = view.findViewById(R.id.new_tab_icon)
        tabsDialogTabCountIcon = view.findViewById(R.id.tab_count_icon)
        tabAdapter = TabAdapter(requireContext(), browser.tabs, dialog, browser)
        tabsRecyclerView.adapter = tabAdapter
        tabsRecyclerView.layoutManager = LinearLayoutManager(context)

        newTabIcon.setOnClickListener {
            dialog.dismiss()
            browser.newTab("https://${URI(browser.searchEngine).host}")
        }

        tabsDialogTabCountIcon.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.window?.also {
            it.attributes.windowAnimations = R.style.SlideUpAndDownAnimationTheme

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.statusBarColor = Color.BLACK
            }
        }
        return dialog
    }

    fun addMenu(v: View, callback: (View) -> Unit) {
        additionalMenuViews.add(v)
        additionalMenuCallbacks.add(callback)
    }

    fun addOptionMenu(menuId: Int, title: String, icon: Drawable, showAsAction: Int, callback: () -> Unit) {
        menu?.add(title)
        menu?.getItem(menuId)?.icon = icon
        menu?.getItem(menuId)?.setShowAsAction(showAsAction)

        optionMenuCallbacks[menuId] = callback
    }

    private fun createOptionMenu(context: Context): AlertDialog {
        val builder = AlertDialog.Builder(context, R.style.BrowserTheme_DialogTheme)
        val root: ViewGroup? = null
        val menuDialogView = LayoutInflater.from(context).inflate(R.layout.browser_layout_options_menu, root, false)
        builder.setView(menuDialogView)
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val wind = dialog.window
        wind?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        wind?.setBackgroundDrawableResource(android.R.color.transparent)
        val params = wind?.attributes
        params?.gravity = Gravity.TOP or Gravity.END
        params?.x = 0
        params?.y = 0
        wind?.attributes = params

        val additionalMenu: ViewGroup = menuDialogView.findViewById(R.id.additionalMenu)

        for (i in additionalMenuViews.indices) {
            additionalMenu.addView(additionalMenuViews[i], 0)
            additionalMenuViews[i].setOnClickListener {
                additionalMenuCallbacks[i].invoke(additionalMenuViews[i])
                menuDialog.dismiss()
            }
        }

        val menuRefreshImage: ImageView = menuDialogView.findViewById(R.id.refreshImage)
        val menuGoForwardImage: ImageView = menuDialogView.findViewById(R.id.goForwardImage)
        val menuShare: TextView = menuDialogView.findViewById(R.id.menuShare)
        val menuBookmarks: TextView = menuDialogView.findViewById(R.id.menuBookmarks)
        val menuExit: TextView = menuDialogView.findViewById(R.id.menuExit)
        val menuBookmarkImage: ImageView = menuDialogView.findViewById(R.id.menuBookmarkImage)
        val menuHistory: TextView = menuDialogView.findViewById(R.id.menuHistory)
        val menuSettings: TextView = menuDialogView.findViewById(R.id.menuSettings)
        val menuFeedback: TextView = menuDialogView.findViewById(R.id.menuFeedback)
        val menuPageInfoImage: ImageView = menuDialogView!!.findViewById(R.id.menuPageInfoImage)

        browser.menuRefreshImage = menuRefreshImage


        menuRefreshImage.setOnClickListener {
            menuDialog.dismiss()

            if (browser.isLoading) {
                browser.currentTab?.fragment?.webview?.stopLoading()
                browser.hideProgressBar()
            } else {
                browser.currentTab?.fragment?.webview?.reload()
            }
        }

        menuGoForwardImage.setOnClickListener {
            menuDialog.dismiss()

            browser.currentTab?.fragment?.webview?.also {
                if (it.canGoForward()) it.goForward()
            }
        }

        menuPageInfoImage.setOnClickListener {
            menuDialog.dismiss()
            protocolDialog.show()

            when {
                browser.sslErroredDomains.contains(URI(browser.url).host) -> {
                    protocolStatusView.text = getString(R.string.unsecure_connection)
                    protocolMessageView.text = getString(R.string.protocol_message_unsecure)
                    protocolDomainView.text = URI(browser.url).host
                    protocolStatusView.setTextColor(ContextCompat.getColor(context, R.color.colorBrowserRed))
                }
                browser.url.startsWith("https://") -> {
                    protocolStatusView.text = getString(R.string.secure_connection)
                    protocolMessageView.text = getString(R.string.protocol_message_secure)
                    protocolDomainView.text = URI(browser.url).host
                    protocolStatusView.setTextColor(ContextCompat.getColor(context, R.color.colorBrowserGreen))
                }
                else -> {
                    protocolStatusView.text = getString(R.string.unsecure_connection)
                    protocolMessageView.text = getString(R.string.protocol_message_unsecure)
                    protocolDomainView.text = URI(browser.url).host
                    protocolStatusView.setTextColor(ContextCompat.getColor(context, R.color.colorBrowserRed))
                }
            }
        }

        menuBookmarkImage.setOnClickListener {
            menuDialog.dismiss()

            thread {
                var bookmarked = false
                var bookmark: Bookmark? = null
                for (bmark in browser.bookmarks) {
                    if (bmark.url == browser.url) {
                        bookmarked = true
                        bookmark = bmark
                    }
                }

                if (bookmarked) {
                    browser.removeBookmark(bookmark!!)
                    onUiThread {
                        Snackbar.make(webviewHolder, "Bookmark removed ${browser.url}", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    bookmark = Bookmark(
                        browser.title,
                        browser.url,
                        getCurrentDateTime().toString("yyyy/MM/dd HH:mm:ss")
                    )
                    browser.putBookmark(bookmark)
                    onUiThread {
                        val snack = Snackbar.make(webviewHolder,
                            "Bookmarked ${bookmark.url}", Snackbar.LENGTH_LONG)
                        snack.setAction("Undo") {
                            thread {
                                browser.removeBookmark(bookmark)

                                onUiThread {
                                    Snackbar.make(webviewHolder, "Bookmark removed ${bookmark.url}", Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        }
                        snack.show()
                    }
                }
            }
        }

        menuShare.setOnClickListener {
            menuDialog.dismiss()

            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                browser.title
            )
            sharingIntent.putExtra(Intent.EXTRA_TEXT, browser.url)
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }

        menuBookmarks.setOnClickListener {
            menuDialog.dismiss()
            bookmarkDialog.show()
        }

        menuHistory.setOnClickListener {
            menuDialog.dismiss()
            historyDialog.show()
        }

        menuSettings.setOnClickListener {
            menuDialog.dismiss()
            settingsDialog.show()
        }

        menuFeedback.setOnClickListener {
            menuDialog.dismiss()
            val uri = Uri.parse("market://details?id=${context.packageName}");
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket);
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=${context.packageName}"))
                )
            }
        }

        menuExit.setOnClickListener {
            activity?.finish()
            menuDialog.dismiss()
        }

        /* Listeners */
        dialog.setOnShowListener {
            thread {
                var bookmarked = false
                for (bookmark in browser.bookmarks) {
                    if (bookmark.url == browser.url) {
                        bookmarked = true
                    }
                }

                onUiThread {
                    if (bookmarked) {
                        menuBookmarkImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_bookmark_full))
                    } else {
                        menuBookmarkImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_bookmark_empty))
                    }
                }
            }

            browser.currentTab?.fragment?.webview?.also {
                if (it.canGoForward()) {
                    menuGoForwardImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_arrow_forward))
                } else {
                    menuGoForwardImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_arrow_forward_disabled))
                }
            }
        }

        return dialog
    }

    private fun openFileWithIntent(context: Context, uri: Uri, mimeType: String, downloadLocalName: String) {
        val snackbar = Snackbar.make(webviewHolder, "download completed for $downloadLocalName", Snackbar.LENGTH_LONG)
        snackbar.setAction("Open") {
            var attachmentUri: Uri? = null
            if (ContentResolver.SCHEME_FILE == uri.scheme && uri.path != null && activity != null) {
                val file = File(uri.path!!)
                attachmentUri = FileProvider.getUriForFile(requireActivity(), "com.gelbintergalactic.fileprovider", file)
            }

            val openAttachmentIntent = Intent(Intent.ACTION_VIEW);
            openAttachmentIntent.setDataAndType(attachmentUri, mimeType);
            openAttachmentIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.startActivity(openAttachmentIntent)
            } catch (e: ActivityNotFoundException) {
                context.toast("Unable to open the file")
            }
        }
        snackbar.show()
    }

    private fun openDownloadedFile(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val downloadLocalName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
            val downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                openFileWithIntent(context, Uri.parse(downloadLocalUri), downloadMimeType, downloadLocalName)
                val file = File(downloadLocalUri)
            }
        }
        cursor.close();
    }

    /* Activity level method */
    fun onBackPressed(): Boolean {
        browser.currentTab?.fragment?.webview?.also {
            if (isOverlayShowing) {
                hideUrlField()
                return true
            } else if (it.canGoBack()) {
                it.goBack()
                return true
            }
        }
        return false
    }

    fun loadWithCustomUserAgent(userAgent: String) {
        browser.loadWithCustomUserAgent(userAgent)
    }


    /* Broadcast receivers */
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null && context == null) return
            val downloadId = intent!!.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            openDownloadedFile(context!!, downloadId)
        }
    }


    /* Override methods */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.browser_menu_browser, menu)
        this.menu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuShowMenu -> menuDialog.show()
            else -> {
                if (item.itemId in optionMenuCallbacks) {
                    optionMenuCallbacks[item.itemId]?.invoke()
                }
            }
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    result?.also {
                        browser.browse(result[0])
                        hideUrlField()
                    }
                }
            }
        }
    }

    override fun onPause() {
        try {
            browser.currentTab?.fragment?.webview?.onPause()
        } catch (e: Exception) {}

        /* unregister broadcasts */
        context?.unregisterReceiver(downloadCompleteReceiver)

        super.onPause()
    }

    override fun onResume() {
        try {
            browser.currentTab?.fragment?.webview?.onResume()
        } catch (e: Exception) {}

        /* register broadcasts */
        context?.registerReceiver(downloadCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        super.onResume()
    }

    override fun onDestroy() {
//        AdblockHelper.get().provider.release()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PermissionHandler.STORAGE_PERMISSION_CODE ||
            requestCode == PermissionHandler.LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PermissionHandler.execute()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
