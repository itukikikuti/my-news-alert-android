package com.itukikikuti.newsalert

/** Shared constants used across the app. */
object Prefs {
    const val PREFS = "news_alert_prefs"
    const val KEY_FCM_TOKEN = "***"
}

/** Intent extras. */
object Extras {
    const val EXTRA_URL = "extra_url"
}

/**
 * Cloudflare Access service-token configuration.
 *
 * The app authenticates to Cloudflare Access with these headers, receives a
 * CF_Authorization cookie, and injects it into the WebView. That lets every
 * page load in the WebView pass Access without an interactive login.
 *
 * Leave ACCESS_HOST empty to disable (e.g. for pure LAN use).
 */
object AccessAuth {
    /** Host used for the initial cookie exchange, e.g. "news.itukikikuti.com". */
    const val ACCESS_HOST = "news.itukikikuti.com"

    /** Service token client id (public part, safe to ship in the APK). */
    const val CLIENT_ID = "ca6db298fa567e23741384ba5ced2235.access"

    /** Service token client secret. */
    const val CLIENT_SECRET = "cfast_gkiCbkD9h1dBhX9jieEHeDH9Sr8Z0BlfwVU7v5Ps948d81e5"
}
