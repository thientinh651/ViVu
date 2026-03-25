//package com.tinh.vivu.data;
//
//import androidx.room.Dao;
//import androidx.room.Delete;
//import androidx.room.Insert;
//import androidx.room.Query;
//
//import com.tinh.vivu.models.RouteStop;
//
//import java.util.List;
//
//@Dao
//public interface RouteStopDao {
//
//    @Insert
//    void insert(RouteStop routeStop);
//
//    @Delete
//    void delete(RouteStop routeStop);
//
//    @Query("SELECT * FROM route_stops ORDER BY orderNumber ASC")
//    List<RouteStop> getAllStops();
//
//    // ĐÂY LÀ HÀM BẠN ĐANG THIẾU: Dùng để lấy điểm dừng của 1 chuyến đi cụ thể
//    @Query("SELECT * FROM route_stops WHERE tripId = :tripId ORDER BY orderNumber ASC")
//    List<RouteStop> getStopsByTripId(int tripId);
//}
package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.RouteStop;

import java.util.List;

@Dao
public interface RouteStopDao {

    @Insert
    void insert(RouteStop routeStop);

    @Update
    void update(RouteStop routeStop);

    @Delete
    void delete(RouteStop routeStop);

    // Lấy điểm dừng theo Trip ID và sắp xếp theo số thứ tự (orderIndex)
    @Query("SELECT * FROM route_stops WHERE tripId = :tripId ORDER BY orderIndex ASC")
    List<RouteStop> getStopsByTripId(int tripId);
}