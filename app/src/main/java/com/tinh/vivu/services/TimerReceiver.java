package com.tinh.vivu.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.tinh.vivu.R;

public class TimerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d("TimerReceiver", "🔔 Đã nhận tín hiệu báo thức!");
            Toast.makeText(context, "Hết giờ! Đang phát báo thức...", Toast.LENGTH_LONG).show();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) return;

            // Đọc âm thanh từ cài đặt
            android.content.SharedPreferences prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE);
            String customSoundUriStr = prefs.getString("alarm_sound_uri", null);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            if (customSoundUriStr != null) {
                try {
                    alarmSound = Uri.parse(customSoundUriStr);
                } catch (Exception e) {
                    Log.e("TimerReceiver", "Lỗi đọc Uri âm thanh, dùng mặc định");
                }
            }

            // Mấu chốt cho Pixel: Luôn tạo Channel ID mới để tránh bị hệ thống ghi nhớ (cache) cấu hình im lặng
            String uniqueChannelId = "REST_TIMER_" + System.currentTimeMillis();

            // Khởi tạo kênh báo thức
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        uniqueChannelId,
                        "Cảnh báo nghỉ ngơi",
                        NotificationManager.IMPORTANCE_HIGH // Bắt buộc HIGH
                );
                channel.setDescription("Cảnh báo khi đến giờ nghỉ ngơi");

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM) // Bắt buộc là ALARM
                        .build();

                channel.setSound(alarmSound, audioAttributes);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000});
                channel.setBypassDnd(true); // Xuyên qua Không làm phiền

                notificationManager.createNotificationChannel(channel);
            }

            // Xây dựng bảng thông báo
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, uniqueChannelId)
                    .setSmallIcon(R.drawable.ic_timer) // QUAN TRỌNG: Phải dùng icon trong thư mục drawable của bạn
                    .setContentTitle("⏰ Đã đến giờ nghỉ ngơi!")
                    .setContentText("Bạn đã lái xe đủ lâu, hãy tấp vào lề và thư giãn một chút nhé!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSound(alarmSound)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000});

            Notification notification = builder.build();
            // Cờ giúp nhạc kêu lặp đi lặp lại không ngừng
            notification.flags |= Notification.FLAG_INSISTENT;

            notificationManager.notify((int) System.currentTimeMillis(), notification);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TimerReceiver", " Lỗi phát thông báo: " + e.getMessage());
        }
    }
}