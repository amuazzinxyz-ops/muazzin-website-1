package com.muazzin.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.app.Notification;
import java.util.Calendar;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "namaaz_reminders_v1";

    @Override public void onReceive(Context context, Intent intent) {
        String prayer = intent.getStringExtra("prayer");
        if (prayer == null) prayer = "Namaaz";
        int id = intent.getIntExtra("id", 100);
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(CHANNEL)==null) {
                Uri sound=android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
                AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                NotificationChannel ch=new NotificationChannel(CHANNEL,"Namaaz reminders",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Prayer time notifications with sound");
                ch.setSound(sound,attrs);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }
        }
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(context,1,open,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n=new NotificationCompat.Builder(context,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(prayer + " reminder")
            .setContentText("Namaaz ka waqt ho gaya hai.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(content)
            .setDefaults(Notification.DEFAULT_ALL)
            .build();
        nm.notify(id,n);
        // Schedule the next daily occurrence.
        android.content.SharedPreferences p=context.getSharedPreferences("prayer_reminders",Context.MODE_PRIVATE);
        int index=id-100;
        if(index>=0 && index<5 && p.getBoolean("e"+index,true)) {
            int h=p.getInt("h"+index,5), m=p.getInt("m"+index,0);
            Calendar next=Calendar.getInstance();
            next.set(Calendar.HOUR_OF_DAY,h); next.set(Calendar.MINUTE,m); next.set(Calendar.SECOND,0); next.set(Calendar.MILLISECOND,0);
            if(next.getTimeInMillis()<=System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR,1);
            Intent again=new Intent(context,AlarmReceiver.class);
            again.putExtra("prayer",prayer); again.putExtra("id",id);
            PendingIntent pi=PendingIntent.getBroadcast(context,id,again,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
        }
    }
}
