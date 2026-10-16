package com.muazzin.reminder;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String[] NAMES = {"Fajr", "Zohar", "Asr", "Maghrib", "Isha"};
    private static final int[] COLORS = {0xff218838, 0xff1769aa, 0xff7952b3, 0xffe87516, 0xffc62828};
    private final int[] hours = new int[5];
    private final int[] minutes = new int[5];
    private final boolean[] enabled = {true, true, true, true, true};
    private final TextView[] timeViews = new TextView[5];
    private final Button[] toggleButtons = new Button[5];
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("prayer_reminders", MODE_PRIVATE);
        for (int i=0;i<5;i++) {
            hours[i] = prefs.getInt("h"+i, new int[]{5,13,16,18,20}[i]);
            minutes[i] = prefs.getInt("m"+i, 0);
            enabled[i] = prefs.getBoolean("e"+i, true);
        }
        buildUi();
        requestNotificationPermission();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(0xfff7f4ed);
        TextView title = new TextView(this);
        title.setText("NAMAAZ REMINDER");
        title.setTextColor(0xff173f36);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView note = new TextView(this);
        note.setText("Set each prayer time. Android will schedule a notification with sound.");
        note.setTextColor(Color.DKGRAY);
        note.setTextSize(14);
        note.setGravity(Gravity.CENTER);
        root.addView(note, new LinearLayout.LayoutParams(-1, dp(54)));

        for (int i=0;i<NAMES.length;i++) {
            final int index=i;
            LinearLayout card = new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(12), dp(8), dp(12), dp(8));
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(66));
            cp.bottomMargin = dp(8);
            root.addView(card, cp);

            TextView name = new TextView(this);
            name.setText(NAMES[i]);
            name.setTextColor(COLORS[i]);
            name.setTextSize(i==0 ? 20 : 18);
            name.setTypeface(null, 1);
            card.addView(name, new LinearLayout.LayoutParams(0, -2, 1));

            timeViews[i] = new TextView(this);
            timeViews[i].setText(timeText(i));
            timeViews[i].setTextColor(0xff222222);
            timeViews[i].setTextSize(17);
            timeViews[i].setGravity(Gravity.CENTER);
            timeViews[i].setOnClickListener(v -> new TimePickerDialog(this, (view,h,m) -> {
                hours[index]=h; minutes[index]=m;
                timeViews[index].setText(timeText(index));
                persist();
                schedule(index);
            }, hours[index], minutes[index], false).show());
            card.addView(timeViews[i], new LinearLayout.LayoutParams(dp(92), -1));

            toggleButtons[i] = new Button(this);
            refreshToggle(i);
            toggleButtons[i].setOnClickListener(v -> {
                enabled[index] = !enabled[index];
                refreshToggle(index);
                persist();
                if (enabled[index]) schedule(index); else cancel(index);
            });
            card.addView(toggleButtons[i], new LinearLayout.LayoutParams(dp(90), dp(46)));
        }

        Button exact = new Button(this);
        exact.setText("Allow exact alarms");
        exact.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else Toast.makeText(this, "Exact alarm permission is not needed on this Android version.", Toast.LENGTH_LONG).show();
        });
        root.addView(exact, new LinearLayout.LayoutParams(-1, dp(50)));

        Button scheduleAll = new Button(this);
        scheduleAll.setText("SAVE & SCHEDULE REMINDERS");
        scheduleAll.setTextColor(Color.WHITE);
        scheduleAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff173f36));
        scheduleAll.setOnClickListener(v -> {
            persist();
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission();
            }
            int count=0;
            for (int i=0;i<5;i++) { if (enabled[i]) { schedule(i); count++; } else cancel(i); }
            Toast.makeText(this, "Saved. Enabled reminders: " + count, Toast.LENGTH_LONG).show();
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(56));
        bp.topMargin=dp(8);
        root.addView(scheduleAll,bp);

        TextView footer = new TextView(this);
        footer.setText("For reliable delivery, allow notifications and exact alarms in Android settings. Some phones also require unrestricted battery usage.");
        footer.setTextColor(Color.DKGRAY);
        footer.setTextSize(12);
        footer.setPadding(0,dp(14),0,0);
        root.addView(footer);
        setContentView(root);
    }

    private String timeText(int i) { return String.format(Locale.getDefault(), "%02d:%02d", hours[i], minutes[i]); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private void refreshToggle(int i) {
        toggleButtons[i].setText(enabled[i] ? "ON" : "OFF");
        toggleButtons[i].setTextColor(Color.WHITE);
        toggleButtons[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(enabled[i] ? 0xff218838 : 0xff777777));
    }
    private void persist() {
        SharedPreferences.Editor e=prefs.edit();
        for(int i=0;i<5;i++){ e.putInt("h"+i,hours[i]); e.putInt("m"+i,minutes[i]); e.putBoolean("e"+i,enabled[i]); }
        e.apply();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
    }
    private void schedule(int i) {
        if (!enabled[i]) return;
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Toast.makeText(this, "Tap Allow exact alarms, then save again.", Toast.LENGTH_LONG).show();
            return;
        }
        Calendar next=Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY,hours[i]); next.set(Calendar.MINUTE,minutes[i]); next.set(Calendar.SECOND,0); next.set(Calendar.MILLISECOND,0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR,1);
        Intent intent=new Intent(this,AlarmReceiver.class);
        intent.putExtra("prayer",NAMES[i]);
        intent.putExtra("id",100+i);
        PendingIntent pi=PendingIntent.getBroadcast(this,100+i,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
    }
    private void cancel(int i) {
        Intent intent=new Intent(this,AlarmReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(this,100+i,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);
    }
    @Override protected void onResume() {
        super.onResume();
        if (prefs != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Reschedule enabled alarms after the exact-alarm permission screen returns.
            for (int i=0;i<5;i++) if (enabled[i]) schedule(i);
        }
    }
}
