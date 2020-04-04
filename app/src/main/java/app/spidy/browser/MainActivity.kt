package app.spidy.browser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import app.spidy.kookaburra.fragments.BrowserFragment

class MainActivity : AppCompatActivity() {
    private lateinit var browserFragment: BrowserFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        browserFragment = BrowserFragment()
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
