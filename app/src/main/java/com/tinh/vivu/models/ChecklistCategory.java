package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "checklist_categories")
public class ChecklistCategory {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int tripId; // Thuộc chuyến đi nào
    private String name; // Tên nhóm: Đồ bảo hộ, Gear, Vehicle...

    public ChecklistCategory(int tripId, String name) {
        this.tripId = tripId;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}