package com.custom.autosplit

import android.app.Presentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout

class CarPresentation(outerContext: Context, display: Display) : Presentation(outerContext, display) {

    private lateinit var wazeWebView: WebView
    private lateinit var ytWebView: WebView
    private lateinit var dividerLine: View
    private lateinit var pointer: ImageView
    private lateinit var rootContainer: View

    private var cursorX = 400f
    private var cursorY = 250f
    private val stepSize = 35f

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideCursorRunnable = Runnable {
        pointer.animate().alpha(0f).setDuration(250).withEndAction {
            pointer.visibility = View.INVISIBLE
        }.start()
    }

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.custom.autosplit.LOAD_URL" -> {
                    val query = intent.getStringExtra("query") ?: return
                    loadYouTubeSearch(query)
                }
                "com.custom.autosplit.SET_MODE" -> {
                    val mode = intent.getStringExtra("mode") ?: "MODE_3"
                    applyDisplayMode(mode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.car_split_display)

        rootContainer = findViewById(R.id.carRootContainer)
        wazeWebView = findViewById(R.id.wazeWebView)
        ytWebView = findViewById(R.id.youtubeWebView)
        dividerLine = findViewById(R.id.dividerLine)
        pointer = findViewById(R.id.virtualPointer)

        setupWebViews()

        val filter = IntentFilter().apply {
            addAction("com.custom.autosplit.LOAD_URL")
            addAction("com.custom.autosplit.SET_MODE")
        }
        context.registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        pointer.x = cursorX
        pointer.y = cursorY
    }

    private fun setupWebViews() {
        configureSettings(wazeWebView)
        configureSettings(ytWebView)

        wazeWebView.loadUrl("https://embed.waze.com/iframe?zoom=14")
        ytWebView.loadUrl("https://m.youtube.com")
    }

    private fun configureSettings(webView: WebView) {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    fun applyDisplayMode(mode: String) {
        val wazeParams = wazeWebView.layoutParams as LinearLayout.LayoutParams
        val ytParams = ytWebView.layoutParams as LinearLayout.LayoutParams

        when (mode) {
            "MODE_1" -> {
                wazeWebView.visibility = View.GONE
                dividerLine.visibility = View.GONE
                ytWebView.visibility = View.VISIBLE
                ytParams.weight = 1.0f
            }
            "MODE_2" -> {
                wazeWebView.visibility = View.VISIBLE
                dividerLine.visibility = View.VISIBLE
                ytWebView.visibility = View.VISIBLE
                wazeParams.weight = 0.70f
                ytParams.weight = 0.30f
            }
            "MODE_3" -> {
                wazeWebView.visibility = View.VISIBLE
                dividerLine.visibility = View.VISIBLE
                ytWebView.visibility = View.VISIBLE
                wazeParams.weight = 0.35f
                ytParams.weight = 0.65f
            }
        }
        wazeWebView.layoutParams = wazeParams
        ytWebView.layoutParams = ytParams
    }

    private fun showPointerTemporarily() {
        hideHandler.removeCallbacks(hideCursorRunnable)
        if (pointer.visibility != View.VISIBLE || pointer.alpha < 1f) {
            pointer.visibility = View.VISIBLE
            pointer.animate().alpha(1f).setDuration(150).start()
        }
        hideHandler.postDelayed(hideCursorRunnable, 3000)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            showPointerTemporarily()

            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_NAVIGATE_NEXT -> {
                    cursorX = (cursorX + stepSize).coerceAtMost(rootContainer.width.toFloat() - 25)
                    updatePointer()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> {
                    cursorX = (cursorX - stepSize).coerceAtLeast(10f)
                    updatePointer()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    cursorY = (cursorY - stepSize).coerceAtLeast(10f)
                    updatePointer()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    cursorY = (cursorY + stepSize).coerceAtMost(rootContainer.height.toFloat() - 25)
                    updatePointer()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    simulateClickAt(cursorX, cursorY)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun updatePointer() {
        pointer.x = cursorX
        pointer.y = cursorY
    }

    private fun simulateClickAt(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis() + 40

        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, eventTime + 40, MotionEvent.ACTION_UP, x, y, 0)

        rootContainer.dispatchTouchEvent(downEvent)
        rootContainer.dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()
    }

    fun loadYouTubeSearch(query: String) {
        val targetUrl = if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else {
            "https://m.youtube.com/results?search_query=${android.net.Uri.encode(query)}"
        }
        ytWebView.loadUrl(targetUrl)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(commandReceiver)
        hideHandler.removeCallbacks(hideCursorRunnable)
    }
}
