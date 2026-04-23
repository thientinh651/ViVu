package com.tinh.vivu.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.tinh.vivu.MainActivity;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.Alarm;
import com.tinh.vivu.services.AlarmReceiver;
import com.tinh.vivu.services.TimerReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class AlarmScheduler {

    public static final String EXTRA_ALARM_ID = "extra_alarm_id";
    public static final String EXTRA_ALARM_LABEL = "extra_alarm_label";
    public static final String EXTRA_ALARM_SOUND_URI = "extra_alarm_sound_uri";
    public static final String EXTRA_ALARM_VIBRATE = "extra_alarm_vibrate";

    private AlarmScheduler() {
    }

    public static boolean canScheduleExactAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        if (alarmManager.canScheduleExactAlarms()) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean scheduleAlarm(Context context, Alarm alarm) {
        long triggerAtMillis = computeNextTriggerTime(alarm, System.currentTimeMillis());
        if (triggerAtMillis <= 0) {
            return false;
        }
        return scheduleExact(context, buildAlarmPendingIntent(context, alarm), triggerAtMillis);
    }

    public static void cancelAlarm(Context context, Alarm alarm) {
        cancelAlarm(context, alarm.getId());
    }

    public static void cancelAlarm(Context context, int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        alarmManager.cancel(buildAlarmPendingIntent(context, alarmId, null, null, true));
    }

    public static boolean scheduleTimer(Context context, long triggerAtMillis) {
        return scheduleExact(context, buildTimerPendingIntent(context), triggerAtMillis);
    }

    public static void cancelTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        alarmManager.cancel(buildTimerPendingIntent(context));
    }

    public static void handleAlarmTriggered(Context context, int alarmId) {
        if (alarmId <= 0) {
            return;
        }

        Context appContext = context.getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Alarm alarm = AppDatabase.getInstance(appContext).alarmDao().getAlarmById(alarmId);
            if (alarm == null) {
                return;
            }

            if (isRecurring(alarm)) {
                scheduleAlarm(appContext, alarm);
            } else {
                alarm.setActive(false);
                AppDatabase.getInstance(appContext).alarmDao().update(alarm);
                cancelAlarm(appContext, alarmId);
            }
        });
    }

    public static void rescheduleActiveAlarms(Context context) {
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Alarm> activeAlarms = AppDatabase.getInstance(appContext).alarmDao().getActiveAlarms();
            for (Alarm alarm : activeAlarms) {
                scheduleAlarm(appContext, alarm);
            }
        });
    }

    public static boolean isRecurring(Alarm alarm) {
        return !getSelectedDays(alarm.getDaysOfWeek()).isEmpty();
    }

    public static long computeNextTriggerTime(Alarm alarm, long nowMillis) {
        Calendar candidate = Calendar.getInstance();
        candidate.setTimeInMillis(nowMillis);
        candidate.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        candidate.set(Calendar.MINUTE, alarm.getMinute());
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);

        List<Integer> selectedDays = getSelectedDays(alarm.getDaysOfWeek());
        if (selectedDays.isEmpty()) {
            if (candidate.getTimeInMillis() <= nowMillis) {
                candidate.add(Calendar.DATE, 1);
            }
            return candidate.getTimeInMillis();
        }

        for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
            Calendar option = (Calendar) candidate.clone();
            option.add(Calendar.DATE, dayOffset);
            if (!selectedDays.contains(option.get(Calendar.DAY_OF_WEEK))) {
                continue;
            }
            if (option.getTimeInMillis() <= nowMillis) {
                continue;
            }
            return option.getTimeInMillis();
        }

        Calendar fallback = (Calendar) candidate.clone();
        do {
            fallback.add(Calendar.DATE, 1);
        } while (!selectedDays.contains(fallback.get(Calendar.DAY_OF_WEEK)));
        return fallback.getTimeInMillis();
    }

    private static boolean scheduleExact(Context context, PendingIntent pendingIntent, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }

        if (scheduleAsAlarmClock(context, alarmManager, pendingIntent, triggerAtMillis)) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
        return true;
    }

    private static boolean scheduleAsAlarmClock(Context context,
                                                AlarmManager alarmManager,
                                                PendingIntent operationIntent,
                                                long triggerAtMillis) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        try {
            PendingIntent showIntent = buildAlarmClockShowIntent(context);
            AlarmManager.AlarmClockInfo alarmClockInfo =
                    new AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent);
            alarmManager.setAlarmClock(alarmClockInfo, operationIntent);
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }

    private static PendingIntent buildAlarmClockShowIntent(Context context) {
        Intent showIntent = new Intent(context, MainActivity.class);
        showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                2001,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent buildAlarmPendingIntent(Context context, Alarm alarm) {
        return buildAlarmPendingIntent(
                context,
                alarm.getId(),
                alarm.getLabel(),
                normalizeSoundUri(alarm.getSoundUri()),
                alarm.isVibrate()
        );
    }

    private static PendingIntent buildAlarmPendingIntent(Context context,
                                                         int alarmId,
                                                         String label,
                                                         String soundUri,
                                                         boolean vibrate) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_ALARM_ID, alarmId);
        intent.putExtra(EXTRA_ALARM_LABEL, label);
        intent.putExtra(EXTRA_ALARM_SOUND_URI, soundUri);
        intent.putExtra(EXTRA_ALARM_VIBRATE, vibrate);
        return PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent buildTimerPendingIntent(Context context) {
        Intent intent = new Intent(context, TimerReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static List<Integer> getSelectedDays(String daysOfWeek) {
        List<Integer> selectedDays = new ArrayList<>();
        if (daysOfWeek == null) {
            return selectedDays;
        }

        String normalizedDays = daysOfWeek.trim();
        if (normalizedDays.isEmpty() || "Once".equalsIgnoreCase(normalizedDays)) {
            return selectedDays;
        }

        if ("Everyday".equalsIgnoreCase(normalizedDays)) {
            selectedDays.add(Calendar.MONDAY);
            selectedDays.add(Calendar.TUESDAY);
            selectedDays.add(Calendar.WEDNESDAY);
            selectedDays.add(Calendar.THURSDAY);
            selectedDays.add(Calendar.FRIDAY);
            selectedDays.add(Calendar.SATURDAY);
            selectedDays.add(Calendar.SUNDAY);
            return selectedDays;
        }

        if ("Weekdays".equalsIgnoreCase(normalizedDays)) {
            selectedDays.add(Calendar.MONDAY);
            selectedDays.add(Calendar.TUESDAY);
            selectedDays.add(Calendar.WEDNESDAY);
            selectedDays.add(Calendar.THURSDAY);
            selectedDays.add(Calendar.FRIDAY);
            return selectedDays;
        }

        String[] dayTokens = normalizedDays.split(",");
        for (String token : dayTokens) {
            int day = mapDayToken(token);
            if (day != -1 && !selectedDays.contains(day)) {
                selectedDays.add(day);
            }
        }
        return selectedDays;
    }

    private static int mapDayToken(String token) {
        String normalizedToken = token.trim().toLowerCase(Locale.US);
        switch (normalizedToken) {
            case "mon":
                return Calendar.MONDAY;
            case "tue":
                return Calendar.TUESDAY;
            case "wed":
                return Calendar.WEDNESDAY;
            case "thu":
                return Calendar.THURSDAY;
            case "fri":
                return Calendar.FRIDAY;
            case "sat":
                return Calendar.SATURDAY;
            case "sun":
                return Calendar.SUNDAY;
            default:
                return -1;
        }
    }

    private static String normalizeSoundUri(String soundUri) {
        if (soundUri == null || soundUri.trim().isEmpty() || "Default".equalsIgnoreCase(soundUri.trim())) {
            return null;
        }
        return soundUri;
    }
}
