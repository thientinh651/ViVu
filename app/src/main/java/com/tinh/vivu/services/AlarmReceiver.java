package com.tinh.vivu.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.tinh.vivu.utils.AlarmScheduler;

public class AlarmReceiver extends BroadcastReceiver {

    private static final int FALLBACK_NOTIFICATION_ID = 9201;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        if ("STOP_ALARM".equals(intent.getAction())) {
            Log.d("AlarmReceiver", "Đã tắt báo thức!");
            AlarmPlaybackService.stopAlert(context);
            return;
        }

        int alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        String label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL);
        String soundUri = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_SOUND_URI);
        boolean vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_ALARM_VIBRATE, true);

        boolean started = AlarmPlaybackService.startAlarmAlert(context, label, soundUri, vibrate);
        if (!started) {
            String title = "⏰ " + (label == null || label.trim().isEmpty() ? "Báo thức của bạn!" : label);
            AlarmPlaybackService.showFallbackAlertNotification(
                    context,
                    FALLBACK_NOTIFICATION_ID,
                    title,
                    "Đã đến giờ rồi, dậy thôi!",
                    soundUri,
                    vibrate
            );
        }
        AlarmScheduler.handleAlarmTriggered(context, alarmId);
    }
}