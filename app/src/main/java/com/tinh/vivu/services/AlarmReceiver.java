package com.tinh.vivu.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.tinh.vivu.MainActivity; // Import để có thể mở lại App
import com.tinh.vivu.R;

public class AlarmReceiver extends BroadcastReceiver {

    // ID cố định để dễ dàng tìm và tắt đúng cái thông báo đang kêu
    private static final int ALARM_NOTIFICATION_ID = 9999;

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // 1. NẾU LÀ HÀNH ĐỘNG BẤM VÀO NÚT "TẮT BÁO THỨC"
        if ("STOP_ALARM".equals(intent.getAction())) {
            Log.d("AlarmReceiver", "Đã tắt báo thức!");
            notificationManager.cancel(ALARM_NOTIFICATION_ID); // Hủy thông báo -> tắt chuông
            return;
        }

        // 2. NẾU LÀ LỆNH GỌI BÁO THỨC BÌNH THƯỜNG
        String label = intent.getStringExtra("ALARM_LABEL");
        if (label == null || label.isEmpty()) label = "Báo thức của bạn!";

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        String channelId = "SMART_ALARM_CHANNEL_V2";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Smart Alarm", NotificationManager.IMPORTANCE_HIGH);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM).build();
            channel.setSound(alarmSound, audioAttributes);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000});
            channel.setBypassDnd(true);
            notificationManager.createNotificationChannel(channel);
        }

        // TẠO LỆNH MỞ APP KHI BẤM VÀO THÔNG BÁO
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Thêm cờ để MainActivity biết là vừa đi từ Báo thức vào
        openAppIntent.putExtra("OPEN_ALARM_PAGE", true);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // TẠO LỆNH TẮT BÁO THỨC (Gửi tín hiệu "STOP_ALARM" ngược lại cho chính file này)
        Intent stopIntent = new Intent(context, AlarmReceiver.class);
        stopIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("⏰ " + label)
                .setContentText("Đã đến giờ rồi, dậy thôi!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(alarmSound)
                .setVibrate(new long[]{1000, 1000, 1000, 1000})
                .setAutoCancel(true) // Tự động xóa thông báo khi chạm vào thân
                .setContentIntent(contentIntent) // Sự kiện chạm vào thân
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "TẮT BÁO THỨC", stopPendingIntent); // Sự kiện chạm vào nút Tắt

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_INSISTENT; // Lặp chuông liên tục

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification);
    }
}