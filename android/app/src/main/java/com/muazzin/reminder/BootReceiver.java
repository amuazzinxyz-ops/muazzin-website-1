package com.muazzin.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    private static final String[] NAMES={"Fajr","Zohar","Asr","Maghrib","Isha"};
    @Override public void onReceive(Context context, Intent intent) {
        SharedPreferences p=context.getSharedPreferences("prayer_reminders",Context.MODE_PRIVATE);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        for(int i=0;i<NAMES.length;i++) {
            if(!p.getBoolean("e"+i,true)) continue;
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) continue;
            Calendar next=Calendar.getInstance();
            next.set(Calendar.HOUR_OF_DAY,p.getInt("h"+i,new int[]{5,13,16,18,20}[i]));
            next.set(Calendar.MINUTE,p.getInt("m"+i,0));
            next.set(Calendar.SECOND,0); next.set(Calendar.MILLISECOND,0);
            if(next.getTimeInMillis()<=System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR,1);
            Intent alarm=new Intent(context,AlarmReceiver.class);
            alarm.putExtra("prayer",NAMES[i]); alarm.putExtra("id",100+i);
            PendingIntent pi=PendingIntent.getBroadcast(context,100+i,alarm,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
        }
    }
}
