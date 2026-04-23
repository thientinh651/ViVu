package com.tinh.vivu.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.tinh.vivu.MainActivity;
import com.tinh.vivu.R;

public class AlarmPlaybackService extends Service {

    private static final String TAG = "AlarmPlaybackService";
    private static final String CHANNEL_ID = "ALARM_PLAYBACK_CHANNEL";
    private static final String FALLBACK_CHANNEL_ID = "ALARM_FALLBACK_CHANNEL";
    private static final String ACTION_START_ALERT = "com.tinh.vivu.action.START_ALERT";
    private static final String ACTION_STOP_ALERT = "com.tinh.vivu.action.STOP_ALERT";
    private static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    private static final String EXTRA_TITLE = "extra_title";
    private static final String EXTRA_TEXT = "extra_text";
    private static final String EXTRA_SOUND_URI = "extra_sound_uri";
    private static final String EXTRA_VIBRATE = "extra_vibrate";
    private static final int TIMER_NOTIFICATION_ID = 9998;
    private static final int ALARM_NOTIFICATION_ID = 9999;
    private static final long[] VIBRATION_PATTERN = new long[]{0, 1000, 1000, 1000};

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private int currentNotificationId = ALARM_NOTIFICATION_ID;

    public static boolean startAlarmAlert(Context context, String label, String soundUri, boolean vibrate) {
        return startAlert(
                context,
                ALARM_NOTIFICATION_ID,
                "⏰ " + (label == null || label.trim().isEmpty() ? "Báo thức của bạn!" : label),
                "Đã đến giờ rồi, dậy thôi!",
                soundUri,
                vibrate
        );
    }

    public static boolean startTimerAlert(Context context, String soundUri) {
        return startAlert(
                context,
                TIMER_NOTIFICATION_ID,
                "⏰ Đã đến giờ nghỉ ngơi!",
                "Bạn đã lái xe đủ lâu, hãy tấp vào lề và thư giãn một chút nhé!",
                soundUri,
                true
        );
    }

    public static void stopAlert(Context context) {
        Intent intent = new Intent(context, AlarmPlaybackService.class);
        intent.setAction(ACTION_STOP_ALERT);
        startServiceSafely(context, intent);
    }

    private static boolean startAlert(Context context,
                                      int notificationId,
                                      String title,
                                      String text,
                                      String soundUri,
                                      boolean vibrate) {
        Intent intent = new Intent(context, AlarmPlaybackService.class);
        intent.setAction(ACTION_START_ALERT);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_TEXT, text);
        intent.putExtra(EXTRA_SOUND_URI, soundUri);
        intent.putExtra(EXTRA_VIBRATE, vibrate);
        return startServiceSafely(context, intent);
    }

    private static boolean startServiceSafely(Context context, Intent intent) {
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                appContext.startForegroundService(intent);
                return true;
            } catch (RuntimeException ex) {
                Log.e(TAG, "Không thể startForegroundService khi app đang nền", ex);
                return false;
            }
        } else {
            try {
                appContext.startService(intent);
                return true;
            } catch (RuntimeException ex) {
                Log.e(TAG, "Không thể startService", ex);
                return false;
            }
        }
    }

    public static void showFallbackAlertNotification(Context context,
                                                     int notificationId,
                                                     String title,
                                                     String text,
                                                     String soundUri,
                                                     boolean vibrate) {
        Context appContext = context.getApplicationContext();
        NotificationManager notificationManager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        Uri alertSound = resolveConfiguredSoundUri(soundUri);
        createFallbackNotificationChannel(notificationManager, alertSound);

        Intent openAppIntent = new Intent(appContext, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                appContext,
                10,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, FALLBACK_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(alertSound);
            if (vibrate) {
                builder.setVibrate(VIBRATION_PATTERN);
            }
        }

        notificationManager.notify(notificationId, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP_ALERT.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!ACTION_START_ALERT.equals(action) || intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        currentNotificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, ALARM_NOTIFICATION_ID);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String text = intent.getStringExtra(EXTRA_TEXT);
        String soundUri = intent.getStringExtra(EXTRA_SOUND_URI);
        boolean vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true);

        startForeground(currentNotificationId, buildNotification(title, text));
        startPlayback(soundUri, vibrate);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    private NotificationCompat.Builder createBaseNotificationBuilder(String title, String text) {
        createNotificationChannel();

        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, AlarmPlaybackService.class);
        stopIntent.setAction(ACTION_STOP_ALERT);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tắt", stopPendingIntent);
    }

    private android.app.Notification buildNotification(String title, String text) {
        return createBaseNotificationBuilder(title, text).build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alarm Playback",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Phát báo thức và timer khi app đang ở nền");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setBypassDnd(true);
        notificationManager.createNotificationChannel(channel);
    }

    private static void createFallbackNotificationChannel(NotificationManager notificationManager,
                                                          Uri alertSound) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel existingChannel = notificationManager.getNotificationChannel(FALLBACK_CHANNEL_ID);
        if (existingChannel != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                FALLBACK_CHANNEL_ID,
                "Alarm Fallback",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Âm báo dự phòng khi foreground service bị chặn");
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATION_PATTERN);
        channel.setBypassDnd(true);
        channel.setSound(alertSound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        notificationManager.createNotificationChannel(channel);
    }

    private void startPlayback(String soundUriString, boolean shouldVibrate) {
        stopPlayback();
        acquireWakeLock();

        Uri soundUri = resolveConfiguredSoundUri(soundUriString);
        if (!startMediaPlayer(soundUri)) {
            Log.e(TAG, "Không thể phát báo thức");
        }

        if (shouldVibrate) {
            startVibration();
        }
    }

    private boolean startMediaPlayer(Uri soundUri) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setDataSource(this, soundUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return true;
        } catch (Exception firstError) {
            Log.e(TAG, "Lỗi phát âm báo đã chọn, thử lại bằng âm mặc định", firstError);
            releaseMediaPlayer();
            Uri fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (fallbackUri == null || fallbackUri.equals(soundUri)) {
                return false;
            }

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                mediaPlayer.setDataSource(this, fallbackUri);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
                return true;
            } catch (Exception secondError) {
                Log.e(TAG, "Lỗi phát cả âm mặc định", secondError);
                releaseMediaPlayer();
                return false;
            }
        }
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibrator = vibratorManager.getDefaultVibrator();
            }
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }

        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0));
        } else {
            vibrator.vibrate(VIBRATION_PATTERN, 0);
        }
    }

    private static Uri resolveConfiguredSoundUri(String soundUriString) {
        if (soundUriString != null && !soundUriString.trim().isEmpty()) {
            try {
                return Uri.parse(soundUriString);
            } catch (Exception e) {
                Log.e(TAG, "URI nhạc chuông không hợp lệ, dùng mặc định", e);
            }
        }
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri != null) {
            return alarmUri;
        }
        Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (notificationUri != null) {
            return notificationUri;
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    }

    private void stopPlayback() {
        releaseMediaPlayer();
        if (vibrator != null) {
            vibrator.cancel();
        }
        releaseWakeLock();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            return;
        }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            return;
        }

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":alarm_playback");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(10 * 60 * 1000L);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }
}
