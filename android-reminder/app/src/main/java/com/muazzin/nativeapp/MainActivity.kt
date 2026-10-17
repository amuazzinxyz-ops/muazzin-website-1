package com.muazzin.nativeapp

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import org.json.JSONObject
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val names = arrayOf("Fajr / फ़ज्र", "Zohar / ज़ुहर", "Asr / अस्र", "Maghrib / मग़रिब", "Isha / इशा")
    private val keys = arrayOf("fajr", "zohar", "asr", "maghrib", "isha")
    private val prefs by lazy { getSharedPreferences("native_prayer_reminders", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 0)
        }
        val title = TextView(this).apply {
            text = "Namaaz Time App Hanafi Shafai"
            textSize = 20f
            setPadding(4, 8, 4, 8)
        }
        val reminderButton = Button(this).apply {
            text = "Set screen-off reminders / रिमाइंडर सेट करें"
            setOnClickListener { showReminderSettings() }
        }
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(ReminderBridge(), "MuazzinReminderBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.evaluateJavascript("(function(){if(window.__muazzinNativeReminderSync)return;window.__muazzinNativeReminderSync=true;function sync(){try{['namaz','hijri'].forEach(function(t){var k=t==='namaz'?'muazzinAddedNamazReminder':'muazzinAddedHijriReminder';var raw=localStorage.getItem(k)||'{}';if(window.MuazzinReminderBridge)window.MuazzinReminderBridge.syncReminder(t,raw);});}catch(e){}}sync();setInterval(sync,3000);})();", null)
                }
            }
            loadUrl("https://amuazzinxyz-ops.github.io/muazzin-website-1/")
        }
        root.addView(title)
        root.addView(reminderButton)
        root.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private inner class ReminderBridge {
        @JavascriptInterface
        fun syncReminder(type: String, raw: String) {
            if (type != "namaz" && type != "hijri") return
            val marker = "last_sync_$type"
            if (prefs.getString(marker, null) == raw) return
            val data = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            prefs.edit().putString(marker, raw).apply()
            val id = if (type == "namaz") 5 else 6
            val label = if (type == "namaz") "Namaaz Reminder" else "Hijri Date Reminder"
            AddedReminderScheduler.sync(this@MainActivity, id, label, data)
            if (data.optBoolean("enabled", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
                runOnUiThread {
                    if (!prefs.getBoolean("exact_prompted_for_added_reminders", false)) {
                        prefs.edit().putBoolean("exact_prompted_for_added_reminders", true).apply()
                        try {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                            Toast.makeText(this@MainActivity, "Allow exact alarms for Hijri and Namaaz reminders, then return to Muazzin.", Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {
                            Toast.makeText(this@MainActivity, "Enable Alarms & reminders permission in Android settings.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showReminderSettings() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        root.addView(TextView(this).apply {
            text = "Daily reminder times / रोज़ाना रिमाइंडर का समय"
            textSize = 20f
        })
        keys.forEachIndexed { index, key ->
            val hour = prefs.getInt("${key}_hour", intArrayOf(5, 13, 16, 18, 20)[index])
            val minute = prefs.getInt("${key}_minute", 0)
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(TextView(this).apply {
                text = names[index]
                textSize = 16f
                setPadding(0, 14, 8, 14)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val time = TextView(this).apply {
                textSize = 17f
                setPadding(12, 14, 12, 14)
                text = formatTime(hour, minute)
                setOnClickListener {
                    TimePickerDialog(this@MainActivity, { _, h, m ->
                        prefs.edit().putInt("${key}_hour", h).putInt("${key}_minute", m).apply()
                        text = formatTime(h, m)
                    }, hour, minute, false).show()
                }
            }
            row.addView(time)
            root.addView(row)
        }
        root.addView(Button(this).apply {
            text = "Save and enable / محفوظ کریں اور آن کریں"
            setOnClickListener {
                if (!canScheduleExactAlarms() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")))
                    Toast.makeText(this@MainActivity,
                        "Allow exact alarms, then return and press Save again.",
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermissionIfNeeded()
                    Toast.makeText(this@MainActivity,
                        "Allow notifications, then press Save again.",
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                scheduleAll()
                Toast.makeText(this@MainActivity,
                    "Daily reminders scheduled. Screen-off delivery must be tested on this phone.",
                    Toast.LENGTH_LONG).show()
                showHome()
            }
        })
        root.addView(Button(this).apply {
            text = "Test notification sound / नोटिफ़िकेशन जाँचें"
            setOnClickListener { sendTestNotification() }
        })
        root.addView(Button(this).apply {
            text = "Back / वापस"
            setOnClickListener { showHome() }
        })
        setContentView(root)
    }

    private fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    private fun scheduleAll() {
        keys.forEachIndexed { index, key ->
            val hour = prefs.getInt("${key}_hour", intArrayOf(5, 13, 16, 18, 20)[index])
            val minute = prefs.getInt("${key}_minute", 0)
            AlarmScheduler.schedule(this, index, hour, minute, names[index])
        }
    }

    private fun sendTestNotification() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded()
            Toast.makeText(this, "Allow notifications first, then test again.", Toast.LENGTH_LONG).show()
            return
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, AlarmReceiver.CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Muazzin test reminder")
            .setContentText("Notification sound test / नोटिफ़िकेशन की आवाज़ जाँच")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(9999, notification)
        Toast.makeText(this, "Test notification sent.", Toast.LENGTH_SHORT).show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannelCompat.create(this)
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(hour: Int, minute: Int) =
        String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute)
}

private object NotificationChannelCompat {
    fun create(activity: Activity): android.app.NotificationChannel {
        return android.app.NotificationChannel(
            AlarmReceiver.CHANNEL_ID, "Namaaz reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily prayer reminders with sound"
            setSound(Settings.System.DEFAULT_ALARM_ALERT_URI,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM).build())
            enableVibration(true)
        }
    }
}
