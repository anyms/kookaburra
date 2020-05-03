package app.spidy.kookaburra.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kookaburra.R
import app.spidy.kookaburra.controllers.Browser

class SuggestionAdapter(
    private val context: Context,
    private val suggestions: List<String>,
    private val browser: Browser,
    private val urlField: EditText,
    private val hideUrlField: () -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.browser_layout_suggetion_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = suggestions.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        mainHolder.suggestionView.text = suggestions[position]
        mainHolder.suggestionView.setOnClickListener {
            hideUrlField()
            browser.browse(suggestions[position])
        }
        mainHolder.pushToUrlBarView.setOnClickListener {
            urlField.setText(suggestions[position])
            urlField.setSelection(urlField.text.length)
        }
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val suggestionView: TextView = v.findViewById(R.id.suggestionView)
        val pushToUrlBarView: ImageView = v.findViewById(R.id.pushToUrlBarView)
    }
}