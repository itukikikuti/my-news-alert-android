package com.itukikikuti.newsalert

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val DEFAULT_ADMIN_URL = "https://news.itukikikuti.com/"
    }

    private lateinit var webView: WebView
    private lateinit var tokenView: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tokenPanel: android.view.View
    private lateinit var tokenToggleBtn: android.widget.Button

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "通知が許可されていません", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        tokenView = findViewById(R.id.tokenView)
        progressBar = findViewById(R.id.progressBar)
        tokenPanel = findViewById(R.id.tokenPanel)
        tokenToggleBtn = findViewById(R.id.tokenToggleBtn)
        setupWebView()

        // Token strip starts hidden to keep the WebView full-height; the button
        // in the top bar toggles it.
        tokenToggleBtn.setOnClickListener {
            val show = tokenPanel.visibility != android.view.View.VISIBLE
            tokenPanel.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Ask for notification permission on Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Retrieve the FCM token at startup and show it so it can be pasted
        // into the server's admin UI. Tapping the token copies it.
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                getSharedPreferences(Prefs.PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(Prefs.KEY_FCM_TOKEN, token)
                    .apply()
                showToken(token)
            } else {
                tokenView.text = "FCMトークンの取得に失敗しました"
            }
        }

        // Obtain the Cloudflare Access cookie, then load the admin UI. The
        // cookie is injected into WebView so page navigations and sub-resource
        // loads pass Access without an interactive login.
        val target = intent.getStringExtra(Extras.EXTRA_URL) ?: DEFAULT_ADMIN_URL
        lifecycleScope.launch {
            prepareAccessCookie()
            webView.loadUrl(target)
        }
    }

    private suspend fun prepareAccessCookie() {
        val host = AccessAuth.ACCESS_HOST
        if (host.isBlank()) return
        try {
            val cookie = AccessCookieFetcher.fetchCookie(this, host)
            if (!cookie.isNullOrBlank()) {
                AccessCookieFetcher.injectCookie(host, cookie)
            }
        } catch (e: Exception) {
            // Non-fatal: the WebView will fall back to the Access login page.
        }
    }

    private fun showToken(token: String) {
        tokenView.text = token
        tokenView.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("FCM token", token))
            Toast.makeText(this, "トークンをコピーしました", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Extras.EXTRA_URL)?.let { webView.loadUrl(it) }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = android.view.View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
