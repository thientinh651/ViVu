package com.tinh.vivu.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.JourneyLog;

import java.util.List;

@Dao
public interface JourneyLogDao {
    @Insert
    void insert(JourneyLog log);

    @Update
    void update(JourneyLog log);

    @Delete
    void delete(JourneyLog log);

    @Query("SELECT * FROM journey_logs ORDER BY createdAt DESC")
    LiveData<List<JourneyLog>> getAllLogs();

    @Query("SELECT * FROM journey_logs WHERE content LIKE :searchQuery OR title LIKE :searchQuery ORDER BY createdAt DESC")
    LiveData<List<JourneyLog>> searchLogs(String searchQuery);
}