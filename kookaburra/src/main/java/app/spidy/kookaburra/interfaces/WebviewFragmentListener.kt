package app.spidy.kookaburra.interfaces

import app.spidy.kookaburra.widgets.KookaburraWebView

interface WebviewFragmentListener {
    fun onLoad(webview: KookaburraWebView)
}