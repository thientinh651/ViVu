package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.Alarm;

import java.util.List;

@Dao
public interface AlarmDao {

    @Insert
    void insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    // Lấy toàn bộ danh sách báo thức, sắp xếp theo giờ tăng dần
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    List<Alarm> getAllAlarms();

    // Lấy danh sách các báo thức đang được BẬT
    @Query("SELECT * FROM alarms WHERE isActive = 1")
    List<Alarm> getActiveAlarms();

    // Lấy 1 báo thức cụ thể theo ID
    @Query("SELECT * FROM alarms WHERE id = :alarmId LIMIT 1")
    Alarm getAlarmById(int alarmId);
}