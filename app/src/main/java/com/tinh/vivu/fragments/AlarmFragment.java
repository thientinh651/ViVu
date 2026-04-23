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
import com.tinh.vivu.utils.AlarmScheduler;
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
    private Context appContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        rvAlarms = view.findViewById(R.id.rv_alarms);
        executorService = Executors.newSingleThreadExecutor();
        database = AppDatabase.getInstance(requireContext());
        appContext = requireContext().getApplicationContext();

        // XIN QUYỀN THÔNG BÁO CHO ANDROID 13+ ĐỂ ĐẢM BẢO HIỆN POPUP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        setupRecyclerView();
        loadAlarms(false);

        view.findViewById(R.id.btn_add_alarm).setOnClickListener(v -> {
            AddAlarmBottomSheet bottomSheet = AddAlarmBottomSheet.newInstance(null);
            bottomSheet.setOnAlarmAddedListener(() -> loadAlarms(true));
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
                    AlarmScheduler.cancelAlarm(appContext, alarmToDelete);
                    loadAlarms(false);
                });
                Toast.makeText(requireContext(), R.string.alarm_toast_deleted, Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(rvAlarms);
    }

    private void loadAlarms(boolean promptForExactPermission) {
        executorService.execute(() -> {
            List<Alarm> alarms = database.alarmDao().getAllAlarms();
            boolean needsExactPermission = false;

            for (Alarm alarm : alarms) {
                if (alarm.isActive()) {
                    boolean scheduled = AlarmScheduler.scheduleAlarm(appContext, alarm);
                    if (!scheduled) {
                        needsExactPermission = true;
                    }
                } else {
                    AlarmScheduler.cancelAlarm(appContext, alarm);
                }
            }

            if (promptForExactPermission && needsExactPermission && getActivity() != null) {
                getActivity().runOnUiThread(this::showExactAlarmPermissionPrompt);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    alarmAdapter.setAlarms(alarms, new AlarmAdapter.OnAlarmToggleListener() {
                        @Override
                        public void onToggle(Alarm alarm, boolean isChecked) {
                            if (isChecked) {
                                if (!ensureExactAlarmPermissionForUserAction()) {
                                    alarm.setActive(false);
                                    executorService.execute(() -> database.alarmDao().update(alarm));
                                    loadAlarms(false);
                                    return;
                                }

                                alarm.setActive(true);
                                executorService.execute(() -> database.alarmDao().update(alarm));
                                AlarmScheduler.scheduleAlarm(appContext, alarm);
                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.alarm_toast_enabled_at, alarm.getHour(), alarm.getMinute()),
                                        Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                alarm.setActive(false);
                                executorService.execute(() -> database.alarmDao().update(alarm));
                                AlarmScheduler.cancelAlarm(appContext, alarm);
                            }
                        }

                        @Override
                        public void onItemClick(Alarm alarm) {
                            AddAlarmBottomSheet bottomSheet = AddAlarmBottomSheet.newInstance(alarm);
                            bottomSheet.setOnAlarmAddedListener(() -> loadAlarms(true));
                            bottomSheet.show(getParentFragmentManager(), "EditAlarmBottomSheet");
                        }
                    });
                });
            }
        });
    }

    private boolean ensureExactAlarmPermissionForUserAction() {
        if (AlarmScheduler.canScheduleExactAlarms(appContext)) {
            return true;
        }
        showExactAlarmPermissionPrompt();
        return false;
    }

    private void showExactAlarmPermissionPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || getActivity() == null) {
            return;
        }
        Toast.makeText(requireContext(), R.string.alarm_exact_permission_message, Toast.LENGTH_LONG).show();
        Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        startActivity(permissionIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }
}