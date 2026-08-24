package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val title =
            intent.getStringExtra("title")
                ?: "चंद्र सूर्य अलार्म"

        val message =
            intent.getStringExtra("message")
                ?: "पंचांग बदल झाला आहे."

        val id =
            intent.getIntExtra("id", 1)

        showNotification(
            context,
            title,
            message,
            id
        )

        // Test alarms are one-shot. Do not run the heavy Panchang
        // calculation again immediately after a test notification.
        if (id in 1..3 || id in 11..13 || id in 21..27) {
            AlarmScheduler(context).scheduleAll()
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        id: Int
    ) {

        val channelId =
            "chandra_alarm_channel_v2"

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val soundUri =
            Uri.parse(
                "android.resource://${context.packageName}/raw/alarm"
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val audioAttributes =
                AudioAttributes.Builder()
                    .setUsage(
                        AudioAttributes.USAGE_ALARM
                    )
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()

            val channel =
                NotificationChannel(
                    channelId,
                    "चंद्र सूर्य अलार्म",
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.description =
                "राशी, नक्षत्र, चरण आणि पंचांग बदल अलार्म"

            channel.enableVibration(true)

            channel.setSound(
                soundUri,
                audioAttributes
            )

            notificationManager.createNotificationChannel(
                channel
            )
        }

        val notification =
            NotificationCompat.Builder(
                context,
                channelId
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    NotificationCompat.CATEGORY_ALARM
                )
                .setAutoCancel(true)
                .build()

        notificationManager.notify(
            id,
            notification
        )
    }
}
