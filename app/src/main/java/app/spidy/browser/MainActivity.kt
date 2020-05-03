package app.spidy.browser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import app.spidy.kookaburra.fragments.BrowserFragment
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var browserFragment: BrowserFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        browserFragment = BrowserFragment()
        browserFragment.browserListener = BrowserListener()
        supportFragmentManager.beginTransaction()
            .add(R.id.browser_holder, browserFragment)
            .commit()

        thread {
            Thread.sleep(10000)

            runOnUiThread {
                browserFragment.currentTab?.fragment?.webview?.evaluateJavascript("""
                    (function() {
                        var parent = document.querySelector("input[name='id']").parentNode;
                        parent.removeAttribute("target");
                        var button = document.createElement("input");
                        button.setAttribute("type", "submit");
                        button.value = "PLAY THE VIDEO";
                        parent.appendChild(button);
                    })();
                """.trimIndent()){}
            }
        }
    }

    override fun onBackPressed() {
        if (!browserFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
