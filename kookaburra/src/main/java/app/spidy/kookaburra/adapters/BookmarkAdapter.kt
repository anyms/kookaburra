package app.spidy.kookaburra.adapters

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kookaburra.R
import app.spidy.kookaburra.controllers.Browser
import app.spidy.kookaburra.data.Bookmark
import app.spidy.kotlinutils.onUiThread
import kotlin.concurrent.thread

class BookmarkAdapter(
    private val context: Context,
    private val bookmarks: ArrayList<Bookmark>,
    private val dialog: Dialog,
    private val browser: Browser
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.browser_layout_bookmark_and_history_item, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.bookmarkTitleView.text = bookmark.title
        holder.bookmarkUrlView.text = bookmark.url

        holder.bookmarkMenuImage.setOnClickListener {
            val popupMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                PopupMenu(context, holder.bookmarkMenuImage, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)
            } else {
                PopupMenu(context, holder.bookmarkMenuImage)
            }
            popupMenu.inflate(R.menu.menu_bookmark_item)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.bookmark_open -> {
                        dialog.dismiss()
                        browser.currentTab?.fragment?.webview?.loadUrl(bookmarks[position].url)
                    }
                    R.id.bookmark_open_new_tab -> {
                        dialog.dismiss()
                        browser.newTab(bookmarks[position].url)
                    }
                    R.id.bookmark_remove -> {
                        thread {
                            browser.removeBookmark(bookmarks[position])
                            onUiThread {
                                bookmarks.remove(bookmarks[position])
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, bookmarks.size)
                            }
                        }
                    }
                    R.id.bookmark_details -> {
                        val alertDialog = AlertDialog.Builder(context).create()
                        alertDialog.setCancelable(false)
                        alertDialog.setMessage("""URL: ${bookmarks[position].url}
                            |Title: ${bookmarks[position].title}
                            |Created on: ${bookmarks[position].createdAt}
                        """.trimMargin())
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Close") { dialog, which ->
                            dialog.dismiss()
                        }
                        alertDialog.show()
                    }
                }

                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }
    }


    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val bookmarkRootLayout: ConstraintLayout = v.findViewById(R.id.bookmark_root_layout)
        val bookmarkTitleView: TextView = v.findViewById(R.id.bookmark_title_view)
        val bookmarkUrlView: TextView = v.findViewById(R.id.bookmark_url_view)
        val bookmarkMenuImage: ImageView = v.findViewById(R.id.bookmark_menu_image)
    }
}