package com.tinh.vivu.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.tinh.vivu.utils.AlarmScheduler;

public class TimerReceiver extends BroadcastReceiver {

    private static final int FALLBACK_NOTIFICATION_ID = 9202;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d("TimerReceiver", "Đã nhận tín hiệu timer");
            android.content.SharedPreferences prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE);
            boolean timerWasRunning = prefs.getBoolean("timerRunning", false);
            long storedEndTime = prefs.getLong("endTime", 0);
            if (!timerWasRunning && storedEndTime == 0) {
                Log.d("TimerReceiver", "Bỏ qua timer trùng vì trạng thái đã được dọn");
                return;
            }
            String customSoundUriStr = prefs.getString("alarm_sound_uri", null);
            prefs.edit()
                    .putLong("millisLeft", 0)
                    .putBoolean("timerRunning", false)
                    .putLong("endTime", 0)
                    .apply();

            AlarmScheduler.cancelTimer(context.getApplicationContext());
            boolean started = AlarmPlaybackService.startTimerAlert(context, customSoundUriStr);
            if (!started) {
                AlarmPlaybackService.showFallbackAlertNotification(
                        context,
                        FALLBACK_NOTIFICATION_ID,
                        "⏰ Đã đến giờ nghỉ ngơi!",
                        "Bạn đã lái xe đủ lâu, hãy tấp vào lề và thư giãn một chút nhé!",
                        customSoundUriStr,
                        true
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TimerReceiver", "Lỗi phát timer: " + e.getMessage());
        }
    }
}