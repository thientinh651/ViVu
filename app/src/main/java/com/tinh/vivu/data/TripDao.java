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

    // THÊM LỆNH XÓA Ở ĐÂY
    @Delete
    void delete(Trip trip);

    // Lấy tất cả các chuyến đi, sắp xếp theo ID giảm dần (mới nhất lên đầu)
    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<Trip> getAllTrips();

    // Lấy thông tin 1 chuyến đi dựa vào ID
    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    Trip getTripById(int tripId);
}