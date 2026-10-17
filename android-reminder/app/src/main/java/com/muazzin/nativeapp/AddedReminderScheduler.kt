package com.muazzin.nativeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AddedReminderScheduler {
    private const val PREFS = "native_prayer_reminders"

    fun sync(context: Context, id: Int, label: String, data: JSONObject) {
        if (id !in 5..6) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "added_$id"
        val enabled = data.optBoolean("enabled", false)
        val time = data.optString("time", "")
        val date = data.optString("date", "")
        val mode = data.optString("mode", "daily")
        val lastFired = data.optString("lastFired", "")
        val enabledAt = data.optLong("enabledAt", 0L)
        prefs.edit()
            .putBoolean("${prefix}_enabled", enabled)
            .putString("${prefix}_time", time)
            .putString("${prefix}_date", date)
            .putString("${prefix}_mode", mode)
            .putString("${prefix}_last_fired", lastFired)
            .putLong("${prefix}_enabled_at", enabledAt)
            .putString("${prefix}_label", label)
            .apply()
        if (!enabled || time.isBlank()) {
            cancel(context, id)
            return
        }
        if (mode == "once" && date.isNotBlank() && lastFired == date) {
            prefs.edit().putBoolean("${prefix}_enabled", false).apply()
            cancel(context, id)
            return
        }
        scheduleFromPrefs(context, id)
    }

    fun restoreAll(context: Context) {
        scheduleFromPrefs(context, 5)
        scheduleFromPrefs(context, 6)
    }

    fun rescheduleAfterAlarm(context: Context, id: Int) {
        if (id !in 5..6) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "added_$id"
        if (prefs.getString("${prefix}_mode", "daily") == "once") {
            prefs.edit().putBoolean("${prefix}_enabled", false).apply()
            cancel(context, id)
        } else {
            scheduleFromPrefs(context, id, forceTomorrow = true)
        }
    }

    private fun scheduleFromPrefs(context: Context, id: Int, forceTomorrow: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "added_$id"
        if (!prefs.getBoolean("${prefix}_enabled", false)) {
            cancel(context, id)
            return
        }
        val time = prefs.getString("${prefix}_time", "") ?: ""
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        if (hour !in 0..23 || minute !in 0..59) return
        val mode = prefs.getString("${prefix}_mode", "daily") ?: "daily"
        val date = prefs.getString("${prefix}_date", "") ?: ""
        val enabledAt = prefs.getLong("${prefix}_enabled_at", 0L)
        val cal = Calendar.getInstance()
        if (mode == "once") {
            val parsed = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date)
            } catch (_: Exception) { null } ?: return
            cal.time = parsed
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis() ||
                (enabledAt > 0L && enabledAt > cal.timeInMillis)) {
                cancel(context, id)
                return
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (forceTomorrow || cal.timeInMillis <= System.currentTimeMillis() ||
                (enabledAt > 0L && enabledAt > cal.timeInMillis)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val label = prefs.getString("${prefix}_label",
            if (id == 5) "Namaaz Reminder" else "Hijri Date Reminder") ?: "Muazzin Reminder"
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", id)
            putExtra("label", label)
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            }
        } catch (_: SecurityException) {
            try { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending) }
            catch (_: Exception) { }
        }
    }

    private fun cancel(context: Context, id: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }
}
