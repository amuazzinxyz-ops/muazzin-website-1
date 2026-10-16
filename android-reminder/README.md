# Muazzin Native Reminder APK

This is a separate Android wrapper and native reminder implementation. It does not edit the existing HTML app, Quran, or Quran Search.

## Build
GitHub Actions workflow: **Build Muazzin Native Reminder APK**. On a successful run, download the **Muazzin-Native-Reminder-APK** artifact.

## Reminder behavior
- Native Android exact alarms, notifications, and alarm-channel sound.
- Daily reminders for Fajr, Zohar, Asr, Maghrib, and Isha; times are configured in the native reminder settings.
- Reminders are rescheduled after device reboot.
- Android may require notification permission, exact-alarm access, and exemption from battery restrictions for reliable delivery.

## Important limitation
The native reminder times are currently separate from the reminder controls inside the HTML page. This first build is a separate native reminder path and does not automatically import the HTML app's saved reminder times. Screen-off behavior must be tested on the target phone and is not claimed verified until tested there.
