package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicles")
public class Vehicle {
    @PrimaryKey(autoGenerate = true)
    private int vehicleId;

    private String licensePlate;
    private int initialOdometer;
    private int currentOdometer;

    public Vehicle(String licensePlate, int initialOdometer, int currentOdometer) {
        this.licensePlate = licensePlate;
        this.initialOdometer = initialOdometer;
        this.currentOdometer = currentOdometer;
    }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public int getInitialOdometer() { return initialOdometer; }
    public void setInitialOdometer(int initialOdometer) { this.initialOdometer = initialOdometer; }

    public int getCurrentOdometer() { return currentOdometer; }
    public void setCurrentOdometer(int currentOdometer) { this.currentOdometer = currentOdometer; }

    @Override
    public String toString() {
        return licensePlate + " • " + currentOdometer + " km";
    }
}
