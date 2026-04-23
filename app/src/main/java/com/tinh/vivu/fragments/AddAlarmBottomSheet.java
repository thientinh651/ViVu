package com.tinh.vivu.fragments;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.Alarm;
import com.tinh.vivu.utils.ValidationUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;

public class AddAlarmBottomSheet extends BottomSheetDialogFragment {
    private static final int MAX_ALARM_LABEL_LENGTH = 40;

    private TimePicker timePicker;
    private LinearLayout layoutDays;
    private EditText etAlarmName;
    private SwitchCompat switchVibrate;
    private boolean[] selectedDays = new boolean[7];
    private final String[] dayTokens = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private String[] dayDisplayNames;

    private static final String REPEAT_EVERYDAY = "Everyday";
    private static final String REPEAT_WEEKDAYS = "Weekdays";
    private static final String REPEAT_ONCE = "Once";

    private Alarm alarmToEdit = null; // Biến lưu báo thức nếu đang ở chế độ SỬA
    private OnAlarmAddedListener listener;

    public interface OnAlarmAddedListener {
        void onAlarmAdded();
    }

    public void setOnAlarmAddedListener(OnAlarmAddedListener listener) {
        this.listener = listener;
    }

    // Hàm tạo dùng chung cho Thêm mới và Sửa
    public static AddAlarmBottomSheet newInstance(Alarm alarm) {
        AddAlarmBottomSheet fragment = new AddAlarmBottomSheet();
        if (alarm != null) {
            Bundle args = new Bundle();
            args.putSerializable("ALARM_OBJ", alarm);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_alarm, container, false);

        timePicker = view.findViewById(R.id.time_picker);
        layoutDays = view.findViewById(R.id.layout_days);
        etAlarmName = view.findViewById(R.id.et_alarm_name);
        switchVibrate = view.findViewById(R.id.switch_vibrate);
        dayDisplayNames = new String[]{
                getString(R.string.alarm_day_mon),
                getString(R.string.alarm_day_tue),
                getString(R.string.alarm_day_wed),
                getString(R.string.alarm_day_thu),
                getString(R.string.alarm_day_fri),
                getString(R.string.alarm_day_sat),
                getString(R.string.alarm_day_sun)
        };

        // Kiểm tra xem có phải chế độ SỬA không
        if (getArguments() != null) {
            alarmToEdit = (Alarm) getArguments().getSerializable("ALARM_OBJ");
        }

        setupDaysUI(view.getContext());
        populateDataIfEditing(); // Đổ dữ liệu cũ vào UI

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_save_bottom).setOnClickListener(v -> saveAlarm());

        return view;
    }

    private void setupDaysUI(android.content.Context context) {
        for (int i = 0; i < 7; i++) {
            final int index = i;
            TextView tvDay = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(4, 0, 4, 0);
            tvDay.setLayoutParams(params);
            tvDay.setText(dayDisplayNames[i]);
            tvDay.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvDay.setPadding(0, 20, 0, 20);
            tvDay.setTextSize(11f);
            tvDay.setTypeface(null, android.graphics.Typeface.BOLD);

            // Mặc định chọn T2-T6 nếu thêm mới
            if (alarmToEdit == null) {
                selectedDays[i] = (i < 5);
            }
            setDaySelectedUI(tvDay, selectedDays[i]);

            tvDay.setOnClickListener(v -> {
                selectedDays[index] = !selectedDays[index];
                setDaySelectedUI(tvDay, selectedDays[index]);
            });

            layoutDays.addView(tvDay);
        }
    }

    // Đổ dữ liệu cũ vào bảng nếu đang SỬA
    private void populateDataIfEditing() {
        if (alarmToEdit == null) return;

        // Vặn đồng hồ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(alarmToEdit.getHour());
            timePicker.setMinute(alarmToEdit.getMinute());
        } else {
            timePicker.setCurrentHour(alarmToEdit.getHour());
            timePicker.setCurrentMinute(alarmToEdit.getMinute());
        }

        // Tên và Rung
        etAlarmName.setText(alarmToEdit.getLabel());
        switchVibrate.setChecked(alarmToEdit.isVibrate());

        // Đổ ngày tháng
        String days = alarmToEdit.getDaysOfWeek();
        if (REPEAT_EVERYDAY.equalsIgnoreCase(days)) {
            for(int i=0; i<7; i++) selectedDays[i] = true;
        } else if (REPEAT_WEEKDAYS.equalsIgnoreCase(days)) {
            for(int i=0; i<7; i++) selectedDays[i] = (i < 5);
        } else if (REPEAT_ONCE.equalsIgnoreCase(days)) {
            for(int i=0; i<7; i++) selectedDays[i] = false;
        } else {
            for (int i=0; i<7; i++) {
                selectedDays[i] = containsDayToken(days, dayTokens[i], dayDisplayNames[i]);
            }
        }

        // Cập nhật lại UI cho các ngày
        for (int i = 0; i < layoutDays.getChildCount(); i++) {
            TextView tv = (TextView) layoutDays.getChildAt(i);
            setDaySelectedUI(tv, selectedDays[i]);
        }
    }

    private void setDaySelectedUI(TextView tv, boolean isSelected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(20f);

        if (isSelected) {
            drawable.setColor(Color.parseColor("#9D76F5"));
            tv.setTextColor(Color.WHITE);
        } else {
            drawable.setColor(Color.parseColor("#E0E0E0"));
            tv.setTextColor(Color.parseColor("#424242"));
        }
        tv.setBackground(drawable);
    }

    private void saveAlarm() {
        // 1. Chốt cứng (final) giờ và phút
        final int hour;
        final int minute;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }

        // 2. Chốt cứng Tên báo thức (label)
        String rawLabel = etAlarmName.getText().toString().trim();
        etAlarmName.setError(null);
        if (!rawLabel.isEmpty()
                && !ValidationUtils.isValidDisplayName(rawLabel, 2, MAX_ALARM_LABEL_LENGTH)) {
            etAlarmName.setError(getString(R.string.alarm_error_invalid_label));
            etAlarmName.requestFocus();
            Toast.makeText(requireContext(), R.string.alarm_error_invalid_label_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        final String label = rawLabel.isEmpty() ? getString(R.string.alarm_default_label) : rawLabel;

        // 3. Tính toán ngày lặp lại
        StringBuilder daysStr = new StringBuilder();
        int count = 0;
        for (int i = 0; i < 7; i++) {
            if (selectedDays[i]) {
                if (daysStr.length() > 0) daysStr.append(", ");
                daysStr.append(dayTokens[i]);
                count++;
            }
        }

        String tempDays = daysStr.toString();
        if (count == 7) tempDays = REPEAT_EVERYDAY;
        else if (count == 5 && selectedDays[0] && selectedDays[1] && selectedDays[2] && selectedDays[3] && selectedDays[4]) tempDays = REPEAT_WEEKDAYS;
        else if (count == 0) tempDays = REPEAT_ONCE;

        // Chốt cứng kết quả chuỗi ngày
        final String finalDays = tempDays;

        // 4. Chốt cứng trạng thái nút Rung (Phải đọc ở ngoài luồng ngầm để tránh lỗi UI Thread)
        final boolean vibrateState = switchVibrate.isChecked();

        // Đưa các biến đã chốt cứng (final) vào luồng chạy ngầm
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (alarmToEdit == null) {
                // THÊM MỚI
                Alarm newAlarm = new Alarm(hour, minute, label, finalDays, true);
                newAlarm.setVibrate(vibrateState);
                AppDatabase.getInstance(requireContext()).alarmDao().insert(newAlarm);
            } else {
                // CẬP NHẬT (SỬA)
                alarmToEdit.setHour(hour);
                alarmToEdit.setMinute(minute);
                alarmToEdit.setLabel(label);
                alarmToEdit.setDaysOfWeek(finalDays);
                alarmToEdit.setActive(true); // Lưu xong thì mặc định bật lên
                alarmToEdit.setVibrate(vibrateState);
                AppDatabase.getInstance(requireContext()).alarmDao().update(alarmToEdit);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(
                            requireContext(),
                            alarmToEdit == null ? R.string.alarm_toast_added : R.string.alarm_toast_updated,
                            Toast.LENGTH_SHORT
                    ).show();
                    if (listener != null) listener.onAlarmAdded();
                    dismiss();
                });
            }
        });
    }

    private boolean containsDayToken(String source, String dayToken, String displayToken) {
        String normalizedSource = source == null ? "" : source.toLowerCase(Locale.ROOT);
        return normalizedSource.contains(dayToken.toLowerCase(Locale.ROOT))
                || normalizedSource.contains(displayToken.toLowerCase(Locale.ROOT));
    }
}