package com.thebackendguy.rentflow.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thebackendguy.rentflow.data.NotificationStore

/**
 * Receives pushes from Firebase. While the app is in the background, Android shows
 * "notification" pushes by itself; this only runs for them when the app is open,
 * and for "data" pushes (title and body keys) at any time.
 */
class RentFlowMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        if (title == null && body == null) return
        Notifications.show(this, title, body)
        // The app is open, so the bell's count goes up straight away
        NotificationStore.refreshCount()
    }

    // A new install or a reset token. It reaches the server the next time a home screen opens.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        Log.d(Notifications.TAG, "New FCM token: $token")
    }
}
