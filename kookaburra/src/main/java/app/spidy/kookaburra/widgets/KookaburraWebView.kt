package app.spidy.kookaburra.widgets

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.webkit.WebView
import android.widget.FrameLayout
import com.bartoszlipinski.viewpropertyobjectanimator.ViewPropertyObjectAnimator
import com.google.android.material.appbar.AppBarLayout


class KookaburraWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    WebView(context.createConfigurationContext(Configuration()), attrs, defStyle) {

    private var gestureDetector: GestureDetector
    private var toolbar: AppBarLayout? = null
    private lateinit var webviewHolder: FrameLayout


    init {
        this.gestureDetector = GestureDetector(context, KookaburraGesture())
    }

    fun setActionBar(toolbar: AppBarLayout?, webviewHolder: FrameLayout) {
        this.toolbar = toolbar
        this.webviewHolder = webviewHolder
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        this.gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    /* Kookaburra gesture class */
    private inner class KookaburraGesture: GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null || e2 == null) return false
            if (e1.pointerCount > 1 || e2.pointerCount > 1) return false
            else {
                try {
                    if (e1.y - e2.y > 20) {
                        // hide actionbar
                        toolbar?.also {
                            it.animate().translationY(-it.bottom.toFloat())
                                .setInterpolator(AccelerateInterpolator()).start()

                            ViewPropertyObjectAnimator.animate(webviewHolder).topMargin(0).setDuration(300).start()
                        }
                        return true
                    } else if (e2.y - e1.y > 20) {
                        // show actionbar
                        toolbar?.also {
                            it.animate().translationY(0f)
                                .setInterpolator(DecelerateInterpolator()).start()

                            ViewPropertyObjectAnimator.animate(webviewHolder).topMargin(it.height).setDuration(300).start()
                        }
                        return true
                    }
                } catch (e: Exception) {
                    Log.d("debug_exception", e.message.toString())
                }
                this@KookaburraWebView.invalidate()
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }
}