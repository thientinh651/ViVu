package com.tinh.vivu.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.media.AudioManager;
import android.content.Context;
import android.os.PowerManager;
import android.content.SharedPreferences;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;

import com.tinh.vivu.MainActivity;
import com.tinh.vivu.R;
import com.tinh.vivu.models.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private MediaPlayer mediaPlayer;
    private List<Song> songList;
    private int currentPosition;
    private int resumePosition = 0;
    private final IBinder musicBind = new MusicBinder();

    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";

    private MediaSessionCompat mediaSession;
    private MusicStateListener listener;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    public interface MusicStateListener {
        void onStateChanged();
        void onSongCompleted();
    }

    public void setListener(MusicStateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        currentPosition = -1;
        songList = new ArrayList<>();
        mediaPlayer = new MediaPlayer();
        initMusicPlayer();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setActive(true);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { start(); }
            @Override
            public void onPause() { pause(); }
            @Override
            public void onSkipToNext() { playNext(); }
            @Override
            public void onSkipToPrevious() { playPrev(); }
            @Override
            public void onSeekTo(long pos) { seek((int)pos); }

            // Nhận sự kiện từ custom actions (Shuffle, Repeat) trên notification của Android 13+
            @Override
            public void onCustomAction(String action, android.os.Bundle extras) {
                handleAction(action);
            }
        });

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioFocusChangeListener = focusChange -> {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(1.0f, 1.0f);
                        if (!mediaPlayer.isPlaying()) start();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // Mất quyền âm thanh vĩnh viễn (ví dụ mở 1 app nghe nhạc khác)
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) pause();
                    if (audioManager != null) {
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // Mất quyền tạm thời (có cuộc gọi, mở 1 video ngắn trên youtube)
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) pause();
                    // Lưu ý: Không abandonAudioFocus ở đây để khi tắt video nhạc sẽ tự phát lại (AUDIOFOCUS_GAIN)
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.2f, 0.2f);
                    break;
            }
        };
    }

    private boolean requestAudioFocus() {
        if (audioManager != null) {
            int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return false;
    }

    private void initMusicPlayer() {
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        listener = null;
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_STICKY;
    }

    private void saveLastPlayedState() {
        if (songList != null && currentPosition >= 0 && currentPosition < songList.size()) {
            Song s = songList.get(currentPosition);
            SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("last_song_path", s.getFilePath())
                    .putInt("last_position", getPosition())
                    .apply();
        }
    }

    public void togglePlayPause() {
        if (isPlaying()) {
            pause();
        } else {
            if (currentPosition == -1) {
                SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
                String lastPath = prefs.getString("last_song_path", null);
                resumePosition = prefs.getInt("last_position", 0);

                if (songList != null && !songList.isEmpty()) {
                    int targetIdx = -1;
                    if (lastPath != null) {
                        for (int i = 0; i < songList.size(); i++) {
                            if (songList.get(i).getFilePath().equals(lastPath)) {
                                targetIdx = i;
                                break;
                            }
                        }
                    }
                    if (targetIdx == -1) {
                        targetIdx = (int) (Math.random() * songList.size());
                        resumePosition = 0;
                    }
                    playSong(targetIdx);
                }
            } else {
                start();
            }
        }
    }

    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY_PAUSE:
                togglePlayPause();
                break;
            case ACTION_NEXT:
                playNext();
                break;
            case ACTION_PREV:
                playPrev();
                break;
            case "ACTION_SHUFFLE":
                setShuffle(!isShuffle());
                if (listener != null) listener.onStateChanged();
                showNotification(isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                break;
            case "ACTION_REPEAT":
                setRepeat(!isRepeat());
                if (listener != null) listener.onStateChanged();
                showNotification(isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                break;
        }
    }

    public void setList(List<Song> songs) {
        songList = songs;
    }

    public void playSong(int position) {
        if (songList == null || songList.isEmpty() || position < 0 || position >= songList.size()) return;
        currentPosition = position;
        mediaPlayer.reset();
        try {
            Song playSong = songList.get(currentPosition);
            mediaPlayer.setDataSource(getApplicationContext(), android.net.Uri.parse(playSong.getFilePath()));
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (resumePosition > 0) {
            mp.seekTo(resumePosition);
            resumePosition = 0;
        }
        // Gọi hàm start() để tự động requestAudioFocus trước khi phát nhạc thay vì mp.start()
        start();
        saveLastPlayedState();
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            showNotification(android.R.drawable.ic_media_play);
            if (listener != null) listener.onStateChanged();
            saveLastPlayedState();
        }
        // Loại bỏ lệnh abandonAudioFocus ở đây để giữ chỗ, chờ phát lại
    }

    public void start() {
        if (requestAudioFocus()) {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                showNotification(android.R.drawable.ic_media_pause);
                if (listener != null) listener.onStateChanged();
            }
        }
    }

    public void playNext() {
        if (songList == null || songList.isEmpty()) return;
        if (isShuffle) {
            currentPosition = (int) (Math.random() * songList.size());
        } else {
            currentPosition++;
            if (currentPosition >= songList.size()) currentPosition = 0;
        }
        playSong(currentPosition);
    }

    public void playPrev() {
        if (songList == null || songList.isEmpty()) return;
        currentPosition--;
        if (currentPosition < 0) currentPosition = songList.size() - 1;
        playSong(currentPosition);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mediaPlayer.getCurrentPosition() > 0) {
            mp.reset();
            if (isRepeat) {
                playSong(currentPosition);
            } else {
                playNext();
            }
            if (listener != null) listener.onSongCompleted();
        }
    }

    public int getPosition() {
        if (mediaPlayer != null) return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) return mediaPlayer.getDuration();
        return 0;
    }

    public boolean isPlaying() {
        if (mediaPlayer != null) return mediaPlayer.isPlaying();
        return false;
    }

    public void seek(int position) {
        if (mediaPlayer != null) mediaPlayer.seekTo(position);
    }

    public Song getCurrentSong() {
        if (songList != null && currentPosition >= 0 && currentPosition < songList.size()) {
            return songList.get(currentPosition);
        }
        return null;
    }

    public void setShuffle(boolean shuffle) {
        isShuffle = shuffle;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setRepeat(boolean repeat) {
        isRepeat = repeat;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Khởi chạy phát nhạc ở hình nền");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(int playPauseBtn) {
        Song currSong = getCurrentSong();
        if (currSong == null) return;

        updateMediaSessionState();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // Thêm cờ để gọi onNewIntent() nếu MainActivity đã đang mở sẵn
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("navigate_to", "music");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent prevIntent = new Intent(this, MusicService.class).setAction(ACTION_PREV);
        PendingIntent prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playIntent = new Intent(this, MusicService.class).setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPending = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, MusicService.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent shuffleIntent = new Intent(this, MusicService.class).setAction("ACTION_SHUFFLE");
        PendingIntent shufflePending = PendingIntent.getService(this, 4, shuffleIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent repeatIntent = new Intent(this, MusicService.class).setAction("ACTION_REPEAT");
        PendingIntent repeatPending = PendingIntent.getService(this, 5, repeatIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currSong.getTitle())
                .setContentText(currSong.getArtist())
                .setSmallIcon(R.drawable.ic_music_notes)
                .setContentIntent(pendingIntent)
                // Các actions này vẫn cần thiết cho thiết bị chạy Android cũ (từ 12 trở xuống)
                .addAction(isShuffle ? R.drawable.ic_shuffle : R.drawable.ic_shuffle_simple, "Shuffle", shufflePending)
                .addAction(android.R.drawable.ic_media_previous, "Prev", prevPending)
                .addAction(playPauseBtn, "PlayPause", playPending)
                .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
                .addAction(isRepeat ? R.drawable.ic_repeat_once : R.drawable.ic_repeat, "Repeat", repeatPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1, 2, 3)) // Tương ứng: Prev(1), Play/Pause(2), Next(3)
                .setOngoing(isPlaying())
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        saveLastPlayedState();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        stopForeground(true);
        super.onDestroy();
    }

    private void updateMediaSessionState() {
        Song currSong = getCurrentSong();
        if (currSong == null) return;

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currSong.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currSong.getArtist())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                // .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, ...) can be added if we have bitmaps
                .build());

        int state = isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long position = getPosition();

        // 1. Tạo Builder cho PlaybackState, khai báo có hỗ trợ Shuffle và Repeat
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setState(state, position, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SEEK_TO |
                        PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE |
                        PlaybackStateCompat.ACTION_SET_REPEAT_MODE);

        // 2. Thêm Custom Action Shuffle (Dành cho Android 13+)
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                "ACTION_SHUFFLE",
                "Shuffle",
                isShuffle ? R.drawable.ic_shuffle : R.drawable.ic_shuffle_simple
        ).build());

        // 3. Thêm Custom Action Repeat (Dành cho Android 13+)
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                "ACTION_REPEAT",
                "Repeat",
                isRepeat ? R.drawable.ic_repeat_once : R.drawable.ic_repeat
        ).build());

        // 4. Set state cho mediaSession
        mediaSession.setPlaybackState(stateBuilder.build());
    }
}