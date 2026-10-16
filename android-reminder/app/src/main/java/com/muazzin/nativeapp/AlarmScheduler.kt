package com.muazzin.nativeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {
    fun schedule(context: Context, id: Int, hour: Int, minute: Int, label: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", id)
            putExtra("label", label)
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        }
    }

    fun rescheduleNext(context: Context, id: Int, label: String) {
        val prefs = context.getSharedPreferences("native_prayer_reminders", Context.MODE_PRIVATE)
        val keys = arrayOf("fajr", "zohar", "asr", "maghrib", "isha")
        val defaults = intArrayOf(5, 13, 16, 18, 20)
        if (id !in keys.indices) return
        schedule(context, id, prefs.getInt("${keys[id]}_hour", defaults[id]),
            prefs.getInt("${keys[id]}_minute", 0), label)
    }
}
