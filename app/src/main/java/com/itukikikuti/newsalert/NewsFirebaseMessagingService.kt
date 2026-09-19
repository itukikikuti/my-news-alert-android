package com.itukikikuti.newsalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

/**
 * Receives FCM pushes and posts one notification per article.
 *
 * Android collapses notifications from the same app when they share a tag/group
 * and arrive close together. To keep every article as its own notification we:
 *   - use a fresh, monotonically increasing notification id per message
 *   - never set a group key (setGroup) on the notification
 *   - include a per-message unique tag so replacements never merge
 */
class NewsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "news_alerts"
        private val counter = AtomicInteger(1000)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "News Alert"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""
        val url = message.data["url"]

        showNotification(title, body, url)
    }

    override fun onNewToken(token: String) {
        // Store the token locally so the admin UI can display it for setup.
        getSharedPreferences(Prefs.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(Prefs.KEY_FCM_TOKEN, token)
            .apply()
    }

    private fun showNotification(title: String, body: String, url: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                // Keep each notification visible instead of folding them.
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!url.isNullOrBlank()) putExtra(Extras.EXTRA_URL, url)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            counter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            // No setGroup here: grouping is what folds notifications together.

        val id = counter.incrementAndGet()
        try {
            NotificationManagerCompat.from(this).notify(id, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted yet; nothing to do until the user allows it.
        }
    }
}
