package com.muazzin.nativeapp

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("alarm_id", 0)
        val label = intent.getStringExtra("label") ?: "Namaaz"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        }.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Muazzin reminder: $label")
            .setContentText("Namaaz reminder waqt ho gaya hai. / नमाज़ का समय हो गया है।")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()
        manager.notify(2000 + id, notification)
        AlarmScheduler.rescheduleNext(context, id, label)
    }

    companion object {
        const val CHANNEL_ID = "muazzin_prayer_reminders"
    }
}
