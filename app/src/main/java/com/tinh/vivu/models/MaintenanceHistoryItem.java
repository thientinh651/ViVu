package com.tinh.vivu.models;

public class MaintenanceHistoryItem {
    private int maintId;
    private String typeName;
    private String date;
    private int odoAtMaint;
    private double cost;
    private String note;

    public int getMaintId() { return maintId; }
    public void setMaintId(int maintId) { this.maintId = maintId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getOdoAtMaint() { return odoAtMaint; }
    public void setOdoAtMaint(int odoAtMaint) { this.odoAtMaint = odoAtMaint; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
