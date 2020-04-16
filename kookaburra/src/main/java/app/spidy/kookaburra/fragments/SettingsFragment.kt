package app.spidy.kookaburra.fragments

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.spidy.kookaburra.BuildConfig
import app.spidy.kookaburra.R
import app.spidy.kookaburra.controllers.Browser
import app.spidy.kookaburra.controllers.TinyDB
import app.spidy.kotlinutils.toast


class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        private const val SEARCH_ENGINE_INDEX = "app.spidy.browser.SEARCH_ENGINE_INDEX"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_screen)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tinyDB = TinyDB(requireContext())

        val searchEngine = findPreference<ListPreference>("search_engine")
        val searchEngineValues = resources.getStringArray(R.array.search_engine_values)
        val searchEngineTitles = resources.getStringArray(R.array.search_engine_titles)
        val searchEngineIndex = tinyDB.getInt(SEARCH_ENGINE_INDEX)
        searchEngine?.setValueIndex(searchEngineIndex)
        searchEngine?.summary = searchEngineTitles[searchEngineIndex]

        searchEngine?.setOnPreferenceChangeListener { preference, newValue ->
            val index = searchEngineValues.indexOf(newValue)
            preference.summary = searchEngineTitles[index]
            tinyDB.putInt(SEARCH_ENGINE_INDEX, index)
            tinyDB.putString("search_engine", newValue.toString())
            return@setOnPreferenceChangeListener true
        }

        val disableJavaScript = findPreference<CheckBoxPreference>("disable_javascript")
        val disableJavaScriptValue = tinyDB.getBoolean("disable_javascript")
        disableJavaScript?.isChecked = disableJavaScriptValue

        disableJavaScript?.setOnPreferenceChangeListener { preference, newValue ->
            val isChecked = !(preference as CheckBoxPreference).isChecked
            tinyDB.putBoolean("disable_javascript", isChecked)
            return@setOnPreferenceChangeListener true
        }


        val disableAdblock = findPreference<CheckBoxPreference>("disable_adblock")
        val disableAdblockValue = tinyDB.getBoolean("disable_adblock")
        disableAdblock?.isChecked = disableAdblockValue

        disableAdblock?.setOnPreferenceChangeListener { preference, newValue ->
            val isChecked = !(preference as CheckBoxPreference).isChecked
            tinyDB.putBoolean("disable_adblock", isChecked)
            return@setOnPreferenceChangeListener true
        }


        findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME


        val feedback = findPreference<Preference>("feedback")
        feedback?.setOnPreferenceClickListener {
            val uri = Uri.parse("market://details?id=${requireContext().packageName}");
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket);
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=${requireContext().packageName}"))
                )
            }
            return@setOnPreferenceClickListener true
        }


        val about = findPreference<Preference>("about")
        about?.setOnPreferenceClickListener {
            val alertDialog = AlertDialog.Builder(context).create()
            alertDialog.setCancelable(false)
            alertDialog.setMessage("""Version: ${BuildConfig.VERSION_NAME}
                            |Last Updated: ${Browser.LAST_UPDATED}
                        """.trimMargin())
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Close") { dialog, which ->
                dialog.dismiss()
            }
            alertDialog.show()
            return@setOnPreferenceClickListener true
        }

    }
}