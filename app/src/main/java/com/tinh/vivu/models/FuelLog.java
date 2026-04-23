package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "fuel_logs",
        foreignKeys = {
                @ForeignKey(
                        entity = Trip.class,
                        parentColumns = "id",
                        childColumns = "tripId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Vehicle.class,
                        parentColumns = "vehicleId",
                        childColumns = "vehicleId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("tripId"), @Index("vehicleId")}
)
public class FuelLog {
    @PrimaryKey(autoGenerate = true)
    private int fuelId;

    private int tripId;
    private int vehicleId;
    private int odoReading;
    private double liters;
    private double pricePerLiter;
    private double totalCost;
    private String location;
    private String date;

    public FuelLog(int tripId, int vehicleId, int odoReading, double liters, double pricePerLiter,
                   double totalCost, String location, String date) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.odoReading = odoReading;
        this.liters = liters;
        this.pricePerLiter = pricePerLiter;
        this.totalCost = totalCost;
        this.location = location;
        this.date = date;
    }

    public int getFuelId() { return fuelId; }
    public void setFuelId(int fuelId) { this.fuelId = fuelId; }

    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public int getOdoReading() { return odoReading; }
    public void setOdoReading(int odoReading) { this.odoReading = odoReading; }

    public double getLiters() { return liters; }
    public void setLiters(double liters) { this.liters = liters; }

    public double getPricePerLiter() { return pricePerLiter; }
    public void setPricePerLiter(double pricePerLiter) { this.pricePerLiter = pricePerLiter; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
