package app.spidy.browser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import app.spidy.kookaburra.fragments.BrowserFragment

class MainActivity : AppCompatActivity() {
    private lateinit var browserFragment: BrowserFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        browserFragment = BrowserFragment()
//        browserFragment.browserListener = BrowserListener()

        browserFragment.addMenu(LayoutInflater.from(this).inflate(R.layout.menu_download, null)) { v ->
            Toast.makeText(this, "hello, world", Toast.LENGTH_LONG).show()
        }

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
