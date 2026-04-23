package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.MaintenanceHistoryItem;
import com.tinh.vivu.models.MaintenanceLog;
import com.tinh.vivu.models.MaintenanceStatusItem;
import com.tinh.vivu.models.MaintenanceType;

import java.util.List;

@Dao
public interface MaintenanceDao {
    @Insert
    long insertType(MaintenanceType maintenanceType);

    @Insert
    long insertLog(MaintenanceLog maintenanceLog);

    @Update
    void updateType(MaintenanceType maintenanceType);

    @Query("SELECT * FROM maintenance_types ORDER BY intervalKm ASC, name ASC")
    List<MaintenanceType> getAllMaintenanceTypes();

    @Query("SELECT * FROM maintenance_types WHERE LOWER(name) LIKE '%' || LOWER(:keyword) || '%' LIMIT 1")
    MaintenanceType findTypeByKeyword(String keyword);

    @Query("SELECT ml.* FROM maintenance_logs ml " +
            "INNER JOIN maintenance_types mt ON mt.typeId = ml.typeId " +
            "WHERE ml.vehicleId = :vehicleId AND LOWER(mt.name) LIKE '%nhớt%' " +
            "ORDER BY ml.odoAtMaint DESC, ml.maintId DESC LIMIT 1")
    MaintenanceLog getLatestOilChangeLog(int vehicleId);

    @Query("SELECT mt.typeId AS typeId, mt.name AS name, mt.intervalKm AS intervalKm, " +
            "MAX(ml.odoAtMaint) AS lastMaintenanceOdo " +
            "FROM maintenance_types mt " +
            "LEFT JOIN maintenance_logs ml ON ml.typeId = mt.typeId AND ml.vehicleId = :vehicleId " +
            "GROUP BY mt.typeId, mt.name, mt.intervalKm " +
            "ORDER BY mt.intervalKm ASC, mt.name ASC")
    List<MaintenanceStatusItem> getMaintenanceStatusForVehicle(int vehicleId);

    @Query("SELECT ml.maintId AS maintId, mt.name AS typeName, ml.date AS date, " +
            "ml.odoAtMaint AS odoAtMaint, ml.cost AS cost, ml.note AS note " +
            "FROM maintenance_logs ml " +
            "INNER JOIN maintenance_types mt ON mt.typeId = ml.typeId " +
            "WHERE ml.vehicleId = :vehicleId " +
            "ORDER BY ml.maintId DESC")
    List<MaintenanceHistoryItem> getMaintenanceHistoryForVehicle(int vehicleId);
}
