package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.Vehicle;

import java.util.List;

@Dao
public interface VehicleDao {
    @Insert
    long insert(Vehicle vehicle);

    @Update
    void update(Vehicle vehicle);

    @Delete
    void delete(Vehicle vehicle);

    @Query("SELECT * FROM vehicles ORDER BY vehicleId ASC")
    List<Vehicle> getAllVehicles();

    @Query("SELECT * FROM vehicles WHERE vehicleId = :vehicleId LIMIT 1")
    Vehicle getVehicleById(int vehicleId);

    @Query("SELECT * FROM vehicles ORDER BY vehicleId ASC LIMIT 1")
    Vehicle getFirstVehicle();

    @Query("SELECT v.* FROM vehicles v INNER JOIN trips t ON t.vehicleId = v.vehicleId WHERE t.id = :tripId LIMIT 1")
    Vehicle getVehicleByTripId(int tripId);

    @Query("SELECT COUNT(*) FROM trips WHERE vehicleId = :vehicleId")
    int countTripsUsingVehicle(int vehicleId);
}
