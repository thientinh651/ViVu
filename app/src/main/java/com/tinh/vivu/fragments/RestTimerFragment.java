package com.tinh.vivu.fragments;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
// THÊM CÁC DÒNG IMPORT NÀY
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tinh.vivu.R;
import com.tinh.vivu.services.AlarmPlaybackService;
import com.tinh.vivu.utils.AlarmScheduler;

import java.util.Calendar;
import java.util.Locale;

public class RestTimerFragment extends Fragment {

    private TextView tvCountdown, tvStatus, tvSelectedTime, tvSelectedSound;
    private View btnStartPauseContainer, btnResetContainer, btnSetTime, btnSetSound;
    private ImageView btnStartPauseIcon;
    private Spinner spinnerDuration;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long timeLeftInMillis = 7200000;
    private long selectedDurationInMillis = 7200000;
    private long endTime;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "timer_prefs";
    private Context appContext;

    // Xử lý kết quả trả về khi người dùng chọn nhạc chuông
    private final ActivityResultLauncher<Intent> ringtonePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        prefs.edit().putString("alarm_sound_uri", uri.toString()).apply();
                        updateSoundText(uri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rest_timer, container, false);

        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        appContext = requireContext().getApplicationContext();
        initViews(view);
        setupSpinner();

        // THÊM ĐOẠN NÀY: Xin quyền Thông báo cho Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Nút Play/Pause
        btnStartPauseContainer.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnResetContainer.setOnClickListener(v -> resetTimer());

        // Nút chọn Giờ cụ thể
        btnSetTime.setOnClickListener(v -> showTimePicker());

        // Nút chọn Nhạc chuông
        btnSetSound.setOnClickListener(v -> pickRingtone());

        // Hiển thị tên nhạc chuông đang lưu
        String savedUri = prefs.getString("alarm_sound_uri", null);
        if (savedUri != null) updateSoundText(Uri.parse(savedUri));

        return view;
    }

    private void initViews(View view) {
        tvCountdown = view.findViewById(R.id.tv_countdown);
        tvStatus = view.findViewById(R.id.tv_status);
        tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        tvSelectedSound = view.findViewById(R.id.tv_selected_sound);

        btnStartPauseContainer = view.findViewById(R.id.btn_start_pause_container);
        btnResetContainer = view.findViewById(R.id.btn_reset_container);
        btnStartPauseIcon = view.findViewById(R.id.btn_start_pause_icon);

        btnSetTime = view.findViewById(R.id.btn_set_time);
        btnSetSound = view.findViewById(R.id.btn_set_sound);

        spinnerDuration = view.findViewById(R.id.spinner_duration);
        // ĐÃ XÓA TÌM NÚT btn_back Ở ĐÂY
    }

    private void setupSpinner() {
        String[] durations = getResources().getStringArray(R.array.rest_timer_durations);
        long[] times = {900000, 1800000, 3600000, 7200000, 10800000};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_white, durations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuration.setAdapter(adapter);
        spinnerDuration.setSelection(3);

        spinnerDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isTimerRunning) {
                    selectedDurationInMillis = times[position];
                    timeLeftInMillis = selectedDurationInMillis;
                    tvSelectedTime.setText(R.string.rest_timer_target_time_hint);
                    updateCountDownText();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Hộp thoại chọn giờ
    private void showTimePicker() {
        if (isTimerRunning) {
            Toast.makeText(requireContext(), getString(R.string.rest_timer_toast_pause_to_set_time), Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        // THÊM THEME DIALOG MIN WIDTH ĐỂ CHUYỂN SANG DẠNG CUỘN
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                (view, hourOfDay, selectedMinute) -> {
                    Calendar targetTime = Calendar.getInstance();
                    targetTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    targetTime.set(Calendar.MINUTE, selectedMinute);
                    targetTime.set(Calendar.SECOND, 0);

                    // Nếu giờ chọn đã qua, thì tính cho ngày hôm sau
                    if (targetTime.before(Calendar.getInstance())) {
                        targetTime.add(Calendar.DATE, 1);
                    }

                    timeLeftInMillis = targetTime.getTimeInMillis() - System.currentTimeMillis();
                    selectedDurationInMillis = timeLeftInMillis;

                    String amPm = hourOfDay >= 12
                            ? getString(R.string.common_pm)
                            : getString(R.string.common_am);
                    int hr12 = hourOfDay > 12 ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                    tvSelectedTime.setText(getString(R.string.rest_timer_target_time_format, hr12, selectedMinute, amPm));

                    updateCountDownText();
                }, hour, minute, true); // true = 24h format
        timePickerDialog.show();
    }

    // Chọn âm báo
    private void pickRingtone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.rest_timer_ringtone_picker_title));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);

        String existingUri = prefs.getString("alarm_sound_uri", null);
        if (existingUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUri));
        }

        ringtonePickerLauncher.launch(intent);
    }

    private void updateSoundText(Uri uri) {
        Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), uri);
        if (ringtone != null) {
            tvSelectedSound.setText(getString(R.string.rest_timer_sound_format, ringtone.getTitle(requireContext())));
        }
    }

    private void startTimer() {
        long triggerAtMillis = System.currentTimeMillis() + timeLeftInMillis;
        if (!AlarmScheduler.scheduleTimer(appContext, triggerAtMillis)) {
            showExactAlarmPermissionPrompt();
            return;
        }
        endTime = triggerAtMillis;

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                AlarmScheduler.cancelTimer(appContext);
                clearStoredTimerState();
                updateInterface();
                tvStatus.setText(R.string.rest_timer_status_time_to_rest);
                AlarmPlaybackService.startTimerAlert(appContext, prefs.getString("alarm_sound_uri", null));
            }
        }.start();

        isTimerRunning = true;
        updateInterface();
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        updateInterface();
        AlarmScheduler.cancelTimer(appContext);
        prefs.edit()
                .putLong("millisLeft", timeLeftInMillis)
                .putBoolean("timerRunning", false)
                .putLong("endTime", 0)
                .apply();
    }

    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timeLeftInMillis = selectedDurationInMillis;
        isTimerRunning = false;
        updateCountDownText();
        updateInterface();
        AlarmScheduler.cancelTimer(appContext);
        prefs.edit()
                .putLong("millisLeft", timeLeftInMillis)
                .putBoolean("timerRunning", false)
                .putLong("endTime", 0)
                .apply();
    }

    private void updateCountDownText() {
        int hours = (int) (timeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((timeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(), "00:%02d:%02d", minutes, seconds);
        }
        tvCountdown.setText(timeLeftFormatted);
    }

    private void updateInterface() {
        if (isTimerRunning) {
            tvStatus.setText(R.string.rest_timer_status_running);
            spinnerDuration.setEnabled(false);
            btnSetTime.setEnabled(false);

            // Đổi icon Pause
            btnStartPauseIcon.setImageResource(R.drawable.ic_pause);
        } else {
            tvStatus.setText(R.string.rest_timer_status_ready);
            spinnerDuration.setEnabled(true);
            btnSetTime.setEnabled(true);

            // Đổi icon Play
            btnStartPauseIcon.setImageResource(R.drawable.ic_play);
        }
    }

    private void showExactAlarmPermissionPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        Toast.makeText(requireContext(), getString(R.string.rest_timer_exact_permission_message), Toast.LENGTH_LONG).show();
        Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        startActivity(permissionIntent);
    }

    private void clearStoredTimerState() {
        prefs.edit()
                .putLong("millisLeft", selectedDurationInMillis)
                .putBoolean("timerRunning", false)
                .putLong("endTime", 0)
                .apply();
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("millisLeft", timeLeftInMillis);
        editor.putBoolean("timerRunning", isTimerRunning);
        editor.putLong("endTime", endTime);
        editor.apply();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        isTimerRunning = prefs.getBoolean("timerRunning", false);

        if (isTimerRunning) {
            endTime = prefs.getLong("endTime", 0);
            timeLeftInMillis = endTime - System.currentTimeMillis();

            if (timeLeftInMillis < 0) {
                timeLeftInMillis = 0;
                isTimerRunning = false;
                AlarmScheduler.cancelTimer(appContext);
                clearStoredTimerState();
                updateCountDownText();
                updateInterface();
                tvStatus.setText(R.string.rest_timer_status_time_to_rest);
                AlarmPlaybackService.startTimerAlert(appContext, prefs.getString("alarm_sound_uri", null));
            } else {
                startTimer();
            }
        } else {
            timeLeftInMillis = prefs.getLong("millisLeft", selectedDurationInMillis);
            updateCountDownText();
            updateInterface();
        }
    }
}