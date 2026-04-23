package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.tinh.vivu.models.FuelLog;

import java.util.List;

@Dao
public interface FuelLogDao {
    @Insert
    long insert(FuelLog fuelLog);

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY odoReading ASC, fuelId ASC")
    List<FuelLog> getFuelLogsByVehicle(int vehicleId);

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY fuelId DESC LIMIT 1")
    FuelLog getLatestFuelLogByVehicle(int vehicleId);

    @Query("SELECT COALESCE(SUM(liters), 0) FROM fuel_logs WHERE tripId = :tripId")
    double getTotalLitersByTrip(int tripId);

    @Query("SELECT COALESCE(SUM(totalCost), 0) FROM fuel_logs WHERE tripId = :tripId")
    double getTotalFuelCostByTrip(int tripId);
}
