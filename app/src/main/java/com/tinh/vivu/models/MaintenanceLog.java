package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "maintenance_logs",
        foreignKeys = {
                @ForeignKey(
                        entity = Vehicle.class,
                        parentColumns = "vehicleId",
                        childColumns = "vehicleId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = MaintenanceType.class,
                        parentColumns = "typeId",
                        childColumns = "typeId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("vehicleId"), @Index("typeId")}
)
public class MaintenanceLog {
    @PrimaryKey(autoGenerate = true)
    private int maintId;

    private int vehicleId;
    private int typeId;
    private int odoAtMaint;
    private double cost;
    private String date;
    private String note;

    public MaintenanceLog(int vehicleId, int typeId, int odoAtMaint, double cost, String date, String note) {
        this.vehicleId = vehicleId;
        this.typeId = typeId;
        this.odoAtMaint = odoAtMaint;
        this.cost = cost;
        this.date = date;
        this.note = note;
    }

    public int getMaintId() { return maintId; }
    public void setMaintId(int maintId) { this.maintId = maintId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public int getOdoAtMaint() { return odoAtMaint; }
    public void setOdoAtMaint(int odoAtMaint) { this.odoAtMaint = odoAtMaint; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
