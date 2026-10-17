package com.muazzin.nativeapp

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("native_prayer_reminders", MODE_PRIVATE) }
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            webChromeClient = WebChromeClient()
            addJavascriptInterface(ReminderBridge(), "MuazzinReminderBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.evaluateJavascript(
                        "(function(){if(window.__muazzinNativeReminderSync)return;" +
                        "window.__muazzinNativeReminderSync=true;" +
                        "function sync(){try{['namaz','hijri'].forEach(function(t){" +
                        "var k=t==='namaz'?'muazzinAddedNamazReminder':'muazzinAddedHijriReminder';" +
                        "var raw=localStorage.getItem(k)||'{}';" +
                        "if(window.MuazzinReminderBridge)window.MuazzinReminderBridge.syncReminder(t,raw);" +
                        "});}catch(e){}}" +
                        "sync();setInterval(sync,3000);})();",
                        null
                    )
                }
            }
            loadUrl("https://amuazzinxyz-ops.github.io/muazzin-website-1/")
        }
        setContentView(webView)
    }

    private inner class ReminderBridge {
        @JavascriptInterface
        fun syncReminder(type: String, raw: String) {
            if (type != "namaz" && type != "hijri") return
            val marker = "last_sync_$type"
            val exactPermissionMarker = "last_exact_alarm_allowed_$type"
            val exactPermissionAllowed = canScheduleExactAlarms()
            if (prefs.getString(marker, null) == raw &&
                prefs.getBoolean(exactPermissionMarker, false) == exactPermissionAllowed) return
            val data = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            prefs.edit()
                .putString(marker, raw)
                .putBoolean(exactPermissionMarker, exactPermissionAllowed)
                .apply()
            val id = if (type == "namaz") 5 else 6
            val label = if (type == "namaz") "Namaaz Reminder" else "Hijri Date Reminder"
            AddedReminderScheduler.sync(this@MainActivity, id, label, data)
            if (data.optBoolean("enabled", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
                runOnUiThread {
                    if (!prefs.getBoolean("exact_prompted_for_added_reminders", false)) {
                        prefs.edit().putBoolean("exact_prompted_for_added_reminders", true).apply()
                        try {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                            Toast.makeText(this@MainActivity, "Allow exact alarms for Muazzin reminders, then return to the app.", Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {
                            Toast.makeText(this@MainActivity, "Enable Alarms & reminders permission in Android settings.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                AlarmReceiver.CHANNEL_ID,
                "Muazzin reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Namaaz and Hijri date reminders with sound"
                setSound(
                    Settings.System.DEFAULT_ALARM_ALERT_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                )
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
