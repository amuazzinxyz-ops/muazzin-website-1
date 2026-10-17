package com.muazzin.nativeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val keys = arrayOf("fajr", "zohar", "asr", "maghrib", "isha")
        val names = arrayOf("Fajr / फ़ज्र", "Zohar / ज़ुहर", "Asr / अस्र", "Maghrib / मग़रिब", "Isha / इशा")
        val defaults = intArrayOf(5, 13, 16, 18, 20)
        val prefs = context.getSharedPreferences("native_prayer_reminders", Context.MODE_PRIVATE)
        keys.forEachIndexed { index, key ->
            AlarmScheduler.schedule(context, index,
                prefs.getInt("${key}_hour", defaults[index]),
                prefs.getInt("${key}_minute", 0), names[index])
        }
        AddedReminderScheduler.restoreAll(context)
    }
}
