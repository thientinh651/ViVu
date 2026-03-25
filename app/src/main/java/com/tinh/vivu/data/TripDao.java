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

    // BẠN THÊM ĐOẠN NÀY VÀO ĐỂ APP BIẾT CÁCH CẬP NHẬT TRẠNG THÁI
    @androidx.room.Update
    void update(Trip trip);

    @Delete
    void delete(Trip trip);

    // Lấy tất cả các chuyến đi, sắp xếp theo ID giảm dần (mới nhất lên đầu)
    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<Trip> getAllTrips();

    // Lấy thông tin 1 chuyến đi dựa vào ID
    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    Trip getTripById(int tripId);
}