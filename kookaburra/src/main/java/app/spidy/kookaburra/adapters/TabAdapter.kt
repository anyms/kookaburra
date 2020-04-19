package app.spidy.kookaburra.adapters

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kookaburra.R
import app.spidy.kookaburra.controllers.Browser
import app.spidy.kookaburra.data.Tab

class TabAdapter(
    private val context: Context,
    private val tabs: ArrayList<Tab>,
    private val dialog: Dialog,
    private val browser: Browser
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.browser_layout_tab_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tabs.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder.adapterPosition) {
            0 -> {
                val params = holder.tabRootLayout.layoutParams as RecyclerView.LayoutParams
                params.topMargin = 40
                holder.tabRootLayout.layoutParams = params
            }
            tabs.size - 1 -> {
                val params = holder.tabRootLayout.layoutParams as RecyclerView.LayoutParams
                params.bottomMargin = 40
                holder.tabRootLayout.layoutParams = params
            }
            else -> {
                val params = holder.tabRootLayout.layoutParams as RecyclerView.LayoutParams
                params.bottomMargin = 20
                params.topMargin = 0
                holder.tabRootLayout.layoutParams = params
            }
        }

        if (holder.adapterPosition == browser.needle) {
            holder.tabRootLayout.setBackgroundResource(R.drawable.tab_background_active)
        } else {
            holder.tabRootLayout.setBackgroundResource(R.drawable.tab_background)
        }

        holder.tabTitleView.text = tabs[position].title
        holder.tabUrlView.text = tabs[position].url

        if (tabs[position].favIcon != null) {
            holder.tabFavIcon.setImageBitmap(BitmapFactory.decodeByteArray(tabs[position].favIcon,
                0, tabs[position].favIcon!!.size))
        } else {
            holder.tabFavIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.page_icon))
        }

        holder.tabRootLayout.setOnClickListener {
            dialog.dismiss()
            if (browser.currentTab != null) {
                browser.switchTab(browser.currentTab!!, tabs[position])
            }
        }

        holder.tabCloseImage.setOnClickListener {
            browser.closeTab(tabs[position])
            dialog.dismiss()
        }
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tabRootLayout: ConstraintLayout = v.findViewById(R.id.tab_root_layout)
        val tabTitleView: TextView = v.findViewById(R.id.tab_title_view)
        val tabUrlView: TextView = v.findViewById(R.id.tab_url_view)
        val tabFavIcon: ImageView = v.findViewById(R.id.fav_icon_image)
        val tabCloseImage: ImageView = v.findViewById(R.id.tab_close_icon)
    }
}