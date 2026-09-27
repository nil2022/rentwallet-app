package com.thebackendguy.rentflow.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.thebackendguy.rentflow.MainActivity
import com.thebackendguy.rentflow.R

/** Push notifications from Firebase Cloud Messaging. */
object Notifications {
    const val TAG = "RentFlowPush"

    // Same id as default_notification_channel_id in the manifest, so background pushes land here too
    private const val CHANNEL_ID = "rentflow_updates"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Rent updates", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rent reminders, payments and receipts"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Android 13+ asks the user before an app may show notifications. */
    fun needsPermission(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /**
     * Prints this phone's FCM token to Logcat, for "Send test message" in the Firebase console.
     * Firebase deprecated tokens for installation ids (register/onRegistered) in 25.1.0, but tokens
     * still work and the console test takes one; switch when the backend starts storing ids.
     */
    @Suppress("DEPRECATION")
    fun logToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { Log.d(TAG, "FCM token: $it") }
            .addOnFailureListener { Log.w(TAG, "Could not get the FCM token", it) }
    }

    /** Shows a notification that opens the app when tapped. */
    fun show(context: Context, title: String?, body: String?) {
        if (needsPermission(context)) return
        createChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.rf_logo))
            .setContentTitle(title ?: context.getString(R.string.app_name))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}
