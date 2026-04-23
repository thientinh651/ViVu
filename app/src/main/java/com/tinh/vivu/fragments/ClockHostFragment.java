package com.tinh.vivu.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tinh.vivu.R;

public class ClockHostFragment extends Fragment {

    private TextView tabAlarm, tabTimer;
    private View btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock_host, container, false);

        tabAlarm = view.findViewById(R.id.tab_alarm);
        tabTimer = view.findViewById(R.id.tab_timer);
        btnBack = view.findViewById(R.id.btn_back);
        setActiveTab(tabAlarm);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        tabAlarm.setOnClickListener(v -> {
            setActiveTab(tabAlarm);
            loadFragment(new AlarmFragment());
        });

        tabTimer.setOnClickListener(v -> {
            setActiveTab(tabTimer);
            loadFragment(new RestTimerFragment());
        });

        // Mặc định hiển thị Alarm đầu tiên
        loadFragment(new AlarmFragment());

        return view;
    }

    private void loadFragment(Fragment fragment) {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.clock_content_frame, fragment)
                .commit();
    }

    private void setActiveTab(TextView activeTab) {
        // 1. Reset sạch sẽ cả 2 tab
        resetTab(tabAlarm);
        resetTab(tabTimer);

        // 2. Bật nền trắng và chữ tím cho tab ĐANG ĐƯỢC BẤM
        activeTab.setBackgroundResource(R.drawable.bg_tab_active);
        activeTab.setTextColor(Color.parseColor("#9D76F5"));
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void resetTab(TextView tab) {
        // Lệnh này sẽ xóa triệt để nền trắng của tab bị bỏ chọn
        tab.setBackgroundResource(0);
        tab.setTextColor(Color.parseColor("#9E9E9E")); // Đổi chữ về màu xám
        tab.setTypeface(null, android.graphics.Typeface.NORMAL);
    }
}