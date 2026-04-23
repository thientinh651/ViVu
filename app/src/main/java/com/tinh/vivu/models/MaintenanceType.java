package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "maintenance_types")
public class MaintenanceType {
    @PrimaryKey(autoGenerate = true)
    private int typeId;

    private String name;
    private int intervalKm;

    public MaintenanceType(String name, int intervalKm) {
        this.name = name;
        this.intervalKm = intervalKm;
    }

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getIntervalKm() { return intervalKm; }
    public void setIntervalKm(int intervalKm) { this.intervalKm = intervalKm; }

    @Override
    public String toString() {
        return name + " (" + intervalKm + " km)";
    }
}
