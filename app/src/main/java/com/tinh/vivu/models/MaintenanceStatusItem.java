package com.tinh.vivu.models;

public class MaintenanceStatusItem {
    private int typeId;
    private String name;
    private int intervalKm;
    private Integer lastMaintenanceOdo;

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getIntervalKm() { return intervalKm; }
    public void setIntervalKm(int intervalKm) { this.intervalKm = intervalKm; }

    public Integer getLastMaintenanceOdo() { return lastMaintenanceOdo; }
    public void setLastMaintenanceOdo(Integer lastMaintenanceOdo) { this.lastMaintenanceOdo = lastMaintenanceOdo; }
}
