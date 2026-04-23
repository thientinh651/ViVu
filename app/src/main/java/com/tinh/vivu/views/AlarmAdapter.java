package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.Alarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private List<Alarm> alarmList = new ArrayList<>();
    private OnAlarmToggleListener toggleListener;

    public interface OnAlarmToggleListener {
        void onToggle(Alarm alarm, boolean isChecked);
        void onItemClick(Alarm alarm);
    }

    public void setAlarms(List<Alarm> alarms, OnAlarmToggleListener listener) {
        this.alarmList = alarms;
        this.toggleListener = listener;
        notifyDataSetChanged();
    }

    // HÀM MỚI: Dùng để lấy Alarm khi người dùng vuốt xóa
    public Alarm getAlarmAt(int position) {
        return alarmList.get(position);
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = alarmList.get(position);

        String amPm = alarm.getHour() >= 12 ? "PM" : "AM";
        int hr12 = alarm.getHour() > 12 ? alarm.getHour() - 12 : (alarm.getHour() == 0 ? 12 : alarm.getHour());
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hr12, alarm.getMinute());

        holder.tvTime.setText(timeStr);
        holder.tvAmPm.setText(amPm);
        holder.tvLabel.setText(alarm.getLabel());
        holder.tvDays.setText(alarm.getDaysOfWeek());

        holder.switchAlarm.setOnCheckedChangeListener(null);
        holder.switchAlarm.setChecked(alarm.isActive());

        if (alarm.isActive()) {
            holder.tvTime.setTextColor(Color.parseColor("#2D2A4A"));
            holder.tvAmPm.setTextColor(Color.parseColor("#2D2A4A"));
        } else {
            holder.tvTime.setTextColor(Color.parseColor("#BDBDBD"));
            holder.tvAmPm.setTextColor(Color.parseColor("#BDBDBD"));
        }

        holder.switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alarm.setActive(isChecked);
            if (toggleListener != null) toggleListener.onToggle(alarm, isChecked);

            if (isChecked) {
                holder.tvTime.setTextColor(Color.parseColor("#2D2A4A"));
                holder.tvAmPm.setTextColor(Color.parseColor("#2D2A4A"));
            } else {
                holder.tvTime.setTextColor(Color.parseColor("#BDBDBD"));
                holder.tvAmPm.setTextColor(Color.parseColor("#BDBDBD"));
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (toggleListener != null) toggleListener.onItemClick(alarm);
        });
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvAmPm, tvLabel, tvDays;
        SwitchCompat switchAlarm;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_alarm_time);
            tvAmPm = itemView.findViewById(R.id.tv_am_pm);
            tvLabel = itemView.findViewById(R.id.tv_alarm_label);
            tvDays = itemView.findViewById(R.id.tv_alarm_days);
            switchAlarm = itemView.findViewById(R.id.switch_alarm);
        }
    }
}