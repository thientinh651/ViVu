package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.tinh.vivu.models.RouteStop;

import java.util.List;

@Dao
public interface RouteStopDao {

    @Insert
    void insert(RouteStop routeStop);

    @Delete
    void delete(RouteStop routeStop);

    // CHÚ Ý: Đã bỏ LiveData, chỉ dùng List<RouteStop>
    @Query("SELECT * FROM route_stops ORDER BY orderNumber ASC")
    List<RouteStop> getAllStops();
}