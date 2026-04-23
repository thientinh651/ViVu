package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.tinh.vivu.models.Trip;

import java.util.List;

@Dao
public interface TripDao {

    @Insert
    void insert(Trip trip);

    // ĐOẠN NÀY ĐỂ APP BIẾT CÁCH CẬP NHẬT TRẠNG THÁI
    @androidx.room.Update
    void update(Trip trip);

    @Delete
    void delete(Trip trip);

    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<Trip> getAllTrips();

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    Trip getTripById(int tripId);
}