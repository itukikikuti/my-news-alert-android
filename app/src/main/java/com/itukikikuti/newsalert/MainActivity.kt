package com.itukikikuti.newsalert

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    companion object {
        private const val DEFAULT_ADMIN_URL = "http://10.0.0.99:3334/"
    }

    private lateinit var webView: WebView
    private lateinit var tokenView: TextView

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
        setupWebView()

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

        // Load either the URL from the tapped notification or the admin UI.
        val target = intent.getStringExtra(Extras.EXTRA_URL) ?: DEFAULT_ADMIN_URL
        webView.loadUrl(target)
    }

    private fun showToken(token: String) {
        tokenView.text = "FCMトークン（タップでコピー）:\n$token"
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
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.webViewClient = WebViewClient()
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
