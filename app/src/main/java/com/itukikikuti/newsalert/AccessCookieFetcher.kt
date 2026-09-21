package com.itukikikuti.newsalert

import android.content.Context
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Performs the Cloudflare Access service-token handshake and returns the
 * CF_Authorization cookie so it can be injected into the WebView.
 *
 * WebView cannot attach custom headers to page loads or sub-resource requests,
 * so the reliable approach is: make one plain HTTP request carrying the
 * service-token headers, capture the Set-Cookie value Access returns, and hand
 * that cookie to CookieManager. Every later WebView request then passes Access.
 */
object AccessCookieFetcher {

    private const val COOKIE_NAME = "CF_Authorization"

    /**
     * Fetch the CF_Authorization cookie for [host] using the configured service
     * token. Returns the cookie value, or null when Access is disabled or the
     * exchange fails.
     */
    suspend fun fetchCookie(
        context: Context,
        host: String = AccessAuth.ACCESS_HOST,
        clientId: String = AccessAuth.CLIENT_ID,
        clientSecret: String = AccessAuth.CLIENT_SECRET,
    ): String? = withContext(Dispatchers.IO) {
        if (host.isBlank() || clientId.isBlank() || clientSecret.isBlank()) return@withContext null

        val url = URL("https://$host/")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("CF-Access-Client-Id", clientId)
            setRequestProperty("CF-Access-Client-Secret", clientSecret)
            setRequestProperty("User-Agent", "NewsAlert-Android")
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                // Drain and bail; Access may also hand back the cookie on a 302,
                // so we still scan the headers before giving up.
                conn.errorStream?.close()
            }

            val cookies = conn.headerFields
                ?.filterKeys { it != null && it.equals("Set-Cookie", ignoreCase = true) }
                ?.values
                ?.flatten()
                ?: emptyList()

            cookies.firstOrNull { it.startsWith("$COOKIE_NAME=") }
                ?.substringAfter("$COOKIE_NAME=")
                ?.substringBefore(";")
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** Inject [cookieValue] into WebView's CookieManager for [host]. */
    fun injectCookie(host: String, cookieValue: String) {
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)
        manager.setCookie("https://$host", "$COOKIE_NAME=$cookieValue; path=/; secure")
        manager.flush()
    }
}
