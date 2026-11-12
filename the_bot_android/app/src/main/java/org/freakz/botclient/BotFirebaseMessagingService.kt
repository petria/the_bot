package org.freakz.botclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BotFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "WATER_WATCH") {
            WaterWatchWorker.start(this)
            return
        }
        val channelId = "bot-events"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(channelId, "Bot events", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.notification?.title ?: "The Bot")
            .setContentText(message.notification?.body ?: "New bot event")
            .setContentIntent(android.app.PendingIntent.getActivity(this, message.data["eventId"]?.hashCode() ?: 0,
                android.content.Intent(this, MainActivity::class.java).putExtra("eventId", message.data["eventId"])
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        try { NotificationManagerCompat.from(this).notify(message.data["eventId"]?.hashCode() ?: System.currentTimeMillis().toInt(), notification) } catch (_: SecurityException) { /* Permission can be revoked while delivering. Inbox remains available. */ }
    }
}
