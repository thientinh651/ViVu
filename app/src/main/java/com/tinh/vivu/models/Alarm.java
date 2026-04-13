package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable; // THÊM IMPORT NÀY

@Entity(tableName = "alarms")
public class Alarm implements Serializable { // THÊM implements Serializable
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int hour;
    private int minute;
    private String label;
    private String daysOfWeek;
    private boolean isActive;
    private String soundUri;
    private boolean isVibrate;
    private boolean isSnooze;

    public Alarm(int hour, int minute, String label, String daysOfWeek, boolean isActive) {
        this.hour = hour;
        this.minute = minute;
        this.label = label;
        this.daysOfWeek = daysOfWeek;
        this.isActive = isActive;
        this.isVibrate = true;
        this.isSnooze = true;
        this.soundUri = "Default";
    }

    // --- Các Getters và Setters giữ nguyên ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getSoundUri() { return soundUri; }
    public void setSoundUri(String soundUri) { this.soundUri = soundUri; }

    public boolean isVibrate() { return isVibrate; }
    public void setVibrate(boolean vibrate) { this.isVibrate = vibrate; }

    public boolean isSnooze() { return isSnooze; }
    public void setSnooze(boolean snooze) { this.isSnooze = snooze; }
}