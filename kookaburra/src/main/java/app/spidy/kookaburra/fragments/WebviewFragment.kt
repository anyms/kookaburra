package app.spidy.kookaburra.fragments


import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Base64
import android.view.*
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment

import app.spidy.kookaburra.R
import app.spidy.kookaburra.controllers.Browser
import app.spidy.kookaburra.interfaces.WebviewFragmentListener
import app.spidy.kookaburra.controllers.ChromeClient
import app.spidy.kookaburra.controllers.PermissionHandler
import app.spidy.kookaburra.controllers.WebClient
import app.spidy.kookaburra.widgets.KookaburraWebView
import app.spidy.kotlinutils.onUiThread
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread


class WebviewFragment : Fragment() {

    lateinit var webview: KookaburraWebView
    lateinit var browser: Browser
    var webviewFragmentListener: WebviewFragmentListener? = null
    var webviewId: String? = null
    var urlToLoad: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.browser_fragment_webview, container, false)
        webview = view.findViewById(R.id.webview)

        webviewFragmentListener?.onLoad(webview)

        webview.settings.apply {
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            useWideViewPort = true
            setSupportMultipleWindows(true)
            builtInZoomControls = true
            loadWithOverviewMode = true
            supportZoom()
            displayZoomControls = false
        }
        webview.webChromeClient =
            ChromeClient(requireContext(), browser)
        webview.webViewClient =
            WebClient(requireContext(), browser, activity)

        browser.loadState(webviewId!!, webview) {
            webview.loadUrl(urlToLoad)
        }



        /* Webview downloader */
        webview.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
//            val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
//            val request = DownloadManager.Request(Uri.parse(url))
//            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            downloadManager?.enqueue(request)

            browser.browserListener?.onNewDownload(webview, url, userAgent, contentDisposition, mimetype, contentLength)
        }
        

        /* Context menu */
        var itemInfo: Bundle? = null
        val handler = Handler {
            itemInfo = it.data
            return@Handler true
        }
        webview.setOnLongClickListener {
            registerForContextMenu(webview)

            val hr = (it as WebView).hitTestResult
            val message = handler.obtainMessage()
            val menu = ArrayList<CharSequence>()
            val callbacks = ArrayList<() -> Unit>()
            var title: String? = null

            if (hr.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || hr.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                webview.requestFocusNodeHref(message)
                title = webview.title
                menu.add("Open in new tab")
                callbacks.add {
                    browser.newTab(itemInfo?.get("url").toString())
                }
                menu.add("Copy link address")
                callbacks.add {
                    val clipboardManager = requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clipData = android.content.ClipData
                        .newPlainText("Link address", itemInfo?.get("url").toString())
                    clipboardManager.setPrimaryClip(clipData)

                    Snackbar.make(webview, "Link address copied.", Snackbar.LENGTH_SHORT).show()
                }

                if (hr.type != WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    menu.add("Copy link text")
                    callbacks.add {
                        val clipboardManager = requireContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData
                            .newPlainText("Link text", itemInfo?.get("title").toString())
                        clipboardManager.setPrimaryClip(clipData)

                        Snackbar.make(webview, "Link content copied.", Snackbar.LENGTH_SHORT).show()
                    }
                }

                menu.add("Share link")
                callbacks.add {
                    val sharingIntent = Intent(Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    if (itemInfo?.get("title") != null) {
                        sharingIntent.putExtra(
                            Intent.EXTRA_SUBJECT,
                            itemInfo?.get("title").toString()
                        )
                    }
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, itemInfo?.get("url").toString())
                    startActivity(Intent.createChooser(sharingIntent, "Share via"))
                }
            }

            if (hr.type == WebView.HitTestResult.IMAGE_TYPE || hr.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

                title = hr.extra

                menu.add("Open image in new tab")
                callbacks.add {
                    if (hr.extra != null) {
                        browser.newTab(hr.extra!!.toString())
                    }
                }
                menu.add("Download image")
                callbacks.add {
                    PermissionHandler.requestStorage(view.context, "To download files, the browser require storage permission. \n" +
                            "\n" +
                            " Would you like to grant?") {
                        val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val uri = Uri.parse(hr.extra)
                        val request = DownloadManager.Request(uri)
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, uri.lastPathSegment)
                        downloadManager.enqueue(request)
                    }
                }
                menu.add("Copy image address")
                callbacks.add {
                    if (hr.extra != null) {
                        val clipboardManager = requireContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData
                            .newPlainText("Image link", hr.extra?.toString())
                        clipboardManager.setPrimaryClip(clipData)

                        Snackbar.make(webview, "Image address copied.", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            menu.add("Share image")
            callbacks.add {
                if (hr.extra != null) {
                    Snackbar.make(webview, "Fetching image...", Snackbar.LENGTH_SHORT).show()
                    thread {
                        val fileDir = requireContext().getExternalFilesDir("__images__")

                        assert(fileDir != null)
                        if (!fileDir!!.exists()) {
                            fileDir.mkdirs()
                        }

                        val file = File(fileDir, "share.png")

                        val fOut = FileOutputStream(file)
                        val imageUrl = hr.extra
                        if (imageUrl!!.startsWith("data:")) {
                            val base64EncodedString = imageUrl.substring(imageUrl.indexOf(",") + 1)
                            fOut.write(Base64.decode(base64EncodedString, Base64.DEFAULT))
                        } else {
                            val u = URL(imageUrl)
                            val bitmap = BitmapFactory.decodeStream(u.openConnection().getInputStream())
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
                        }

                        fOut.flush()
                        fOut.close()

                        context?.onUiThread {
                            val imageUri = FileProvider.getUriForFile(requireContext(), requireContext().applicationContext.packageName + ".provider", file)
                            val sharingIntent = Intent(Intent.ACTION_SEND)
                            sharingIntent.type = "image/*"
                            sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            sharingIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                            startActivity(Intent.createChooser(sharingIntent, "Share via"))
                        }
                    }
                }
            }


            if (menu.isNotEmpty()) {
                val builder = AlertDialog.Builder(requireContext())
                if (title != null) {
                    builder.setTitle(title)
                } else {
                    builder.setTitle(webview.url)
                }
                builder.setItems(menu.toArray(arrayOf())) { dialog, index ->
                    callbacks[index]()
                }
                builder.show()
            }

            return@setOnLongClickListener false
        }


        return view
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

    override fun onResume() {
        webview.settings.javaScriptEnabled = browser.isJavaScriptEnabled
//        if (!browser.isAdblockEnabled) {
//            Log.d("hello", "Adblock diabled")
//            AdblockHelper.get().provider.release()
//        } else {
//            if (!AdblockHelper.get().isInit) {
//                Log.d("hello", "Adblock enabled")
//                val basePath = context!!.getDir(AdblockEngine.BASE_PATH_DIRECTORY, Context.MODE_PRIVATE).absolutePath
//                AdblockHelper.get().init(context, basePath, false, AdblockHelper.PREFERENCE_NAME)
//                webview.setProvider(AdblockHelper.get().provider)
//            }
//        }
        super.onResume()
    }
}
