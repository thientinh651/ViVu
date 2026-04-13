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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAlarmBottomSheet extends BottomSheetDialogFragment {

    private TimePicker timePicker;
    private LinearLayout layoutDays;
    private EditText etAlarmName;
    private SwitchCompat switchVibrate;
    private boolean[] selectedDays = new boolean[7];
    private String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

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
            tvDay.setText(dayNames[i]);
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
        if (days.equals("Everyday")) {
            for(int i=0; i<7; i++) selectedDays[i] = true;
        } else if (days.equals("Weekdays")) {
            for(int i=0; i<7; i++) selectedDays[i] = (i < 5);
        } else if (days.equals("Once")) {
            for(int i=0; i<7; i++) selectedDays[i] = false;
        } else {
            for (int i=0; i<7; i++) {
                selectedDays[i] = days.contains(dayNames[i]);
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
        final String label = rawLabel.isEmpty() ? "Alarm" : rawLabel;

        // 3. Tính toán ngày lặp lại
        StringBuilder daysStr = new StringBuilder();
        int count = 0;
        for (int i = 0; i < 7; i++) {
            if (selectedDays[i]) {
                if (daysStr.length() > 0) daysStr.append(", ");
                daysStr.append(dayNames[i]);
                count++;
            }
        }

        String tempDays = daysStr.toString();
        if (count == 7) tempDays = "Everyday";
        else if (count == 5 && selectedDays[0] && selectedDays[1] && selectedDays[2] && selectedDays[3] && selectedDays[4]) tempDays = "Weekdays";
        else if (count == 0) tempDays = "Once";

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
                    Toast.makeText(requireContext(), alarmToEdit == null ? "Đã thêm!" : "Đã sửa!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onAlarmAdded();
                    dismiss();
                });
            }
        });
    }
}