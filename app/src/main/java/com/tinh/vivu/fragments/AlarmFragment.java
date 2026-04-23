package com.tinh.vivu.fragments;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.Alarm;
import com.tinh.vivu.services.AlarmReceiver;
import com.tinh.vivu.views.AlarmAdapter;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmFragment extends Fragment {

    private RecyclerView rvAlarms;
    private AlarmAdapter alarmAdapter;
    private ExecutorService executorService;
    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        rvAlarms = view.findViewById(R.id.rv_alarms);
        executorService = Executors.newSingleThreadExecutor();
        database = AppDatabase.getInstance(requireContext());

        // XIN QUYỀN THÔNG BÁO CHO ANDROID 13+ ĐỂ ĐẢM BẢO HIỆN POPUP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        setupRecyclerView();
        loadAlarms();

        view.findViewById(R.id.btn_add_alarm).setOnClickListener(v -> {
            AddAlarmBottomSheet bottomSheet = AddAlarmBottomSheet.newInstance(null);
            bottomSheet.setOnAlarmAddedListener(this::loadAlarms);
            bottomSheet.show(getParentFragmentManager(), "AddAlarmBottomSheet");
        });

        return view;
    }

    private void setupRecyclerView() {
        alarmAdapter = new AlarmAdapter();
        rvAlarms.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlarms.setAdapter(alarmAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Alarm alarmToDelete = alarmAdapter.getAlarmAt(position);

                executorService.execute(() -> {
                    database.alarmDao().delete(alarmToDelete);
                    cancelAlarmInSystem(alarmToDelete);
                    loadAlarms();
                });
                Toast.makeText(requireContext(), "Đã xóa báo thức", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(rvAlarms);
    }

    private void loadAlarms() {
        executorService.execute(() -> {
            List<Alarm> alarms = database.alarmDao().getAllAlarms();

            for (Alarm alarm : alarms) {
                if (alarm.isActive()) scheduleAlarmInSystem(alarm);
                else cancelAlarmInSystem(alarm);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    alarmAdapter.setAlarms(alarms, new AlarmAdapter.OnAlarmToggleListener() {
                        @Override
                        public void onToggle(Alarm alarm, boolean isChecked) {
                            alarm.setActive(isChecked);
                            executorService.execute(() -> database.alarmDao().update(alarm));

                            if (isChecked) {
                                scheduleAlarmInSystem(alarm);
                                Toast.makeText(requireContext(), "Đã bật báo thức lúc " + alarm.getHour() + ":" + String.format("%02d", alarm.getMinute()), Toast.LENGTH_SHORT).show();
                            } else {
                                cancelAlarmInSystem(alarm);
                            }
                        }

                        @Override
                        public void onItemClick(Alarm alarm) {
                            AddAlarmBottomSheet bottomSheet = AddAlarmBottomSheet.newInstance(alarm);
                            bottomSheet.setOnAlarmAddedListener(() -> loadAlarms());
                            bottomSheet.show(getParentFragmentManager(), "EditAlarmBottomSheet");
                        }
                    });
                });
            }
        });
    }

    private void scheduleAlarmInSystem(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        intent.putExtra("ALARM_LABEL", alarm.getLabel());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        calendar.set(Calendar.MINUTE, alarm.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Nếu giờ chọn đã qua (dù chỉ là vài giây), hẹn luôn qua ngày mai!
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        if (alarmManager != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    } else {
                        // CHỖ NÀY BẮT ÉP PHẢI XIN QUYỀN NẾU CHƯA CÓ
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Vui lòng cấp quyền Báo thức & Nhắc nhở để chuông kêu đúng giờ!", Toast.LENGTH_LONG).show());
                        }
                        Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(permissionIntent);
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } catch (Exception e) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    private void cancelAlarmInSystem(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }
}