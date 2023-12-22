package com.vpnload.vpn.service

import android.annotation.SuppressLint
import android.app.Notification
import com.vpnload.vpn.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vpnload.vpn.ui.SplashScreen


@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessaging : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("TAG", "From: ${remoteMessage.from}")
        remoteMessage.notification?.title?.let {
            remoteMessage.notification!!.body?.let { it1 ->
                sendNotification(
                    it,
                    it1, remoteMessage.data
                )
            }
        };
    }

    private fun sendNotification(title: String, text: String, data: Map<String, String?>) {
        val pendingIntent: PendingIntent
        val intent: Intent = if (data["url"] != null) {
            try {
                Intent(Intent.ACTION_VIEW, Uri.parse(data["url"]))
            } catch (e: Exception) {
                e.printStackTrace()
                Intent(this, SplashScreen::class.java)
            }
        } else {
            Intent(this, SplashScreen::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val mNotificationManager =
            this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val nb: NotificationCompat.Builder = NotificationCompat.Builder(this, getString(R.string.app_name))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                getString(R.string.app_name),
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            mChannel.enableLights(true)
            mChannel.enableVibration(true)
            mNotificationManager.createNotificationChannel(mChannel)
            nb.setChannelId(getString(R.string.app_name) + getString(R.string.app_name))
        }
        nb.setSmallIcon(R.drawable.ic_stat_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nb.color = resources.getColor(R.color.colorPrimary, theme)
        }
        nb.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round))
        nb.setContentTitle(title)
        nb.setContentText(text)
        nb.setAutoCancel(true)
        nb.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000, 1000, 1000))
        nb.priority = Notification.PRIORITY_MAX
        nb.setContentIntent(pendingIntent)
        nb.setChannelId(getString(R.string.app_name))
        mNotificationManager.notify(0, nb.build())
    }
}