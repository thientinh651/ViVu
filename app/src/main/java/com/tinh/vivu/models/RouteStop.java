package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "route_stops")
public class RouteStop {
    @PrimaryKey(autoGenerate = true)
    private int id; // point_id
    private int tripId; // trip_id
    private String locationName;
    private int orderIndex;
    private String expectedArrival;
    private String expectedDeparture;
    private String actualArrival;
    private String actualDeparture;
    private boolean isArrived;


    public RouteStop(int tripId, String locationName, int orderIndex, String expectedArrival, String expectedDeparture) {
        this.tripId = tripId;
        this.locationName = locationName;
        this.orderIndex = orderIndex;
        this.expectedArrival = expectedArrival;
        this.expectedDeparture = expectedDeparture;

        // Mặc định khi mới lên kế hoạch thì thực tế là rỗng và chưa đến nơi
        this.actualArrival = "";
        this.actualDeparture = "";
        this.isArrived = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public String getExpectedArrival() { return expectedArrival; }
    public void setExpectedArrival(String expectedArrival) { this.expectedArrival = expectedArrival; }

    public String getExpectedDeparture() { return expectedDeparture; }
    public void setExpectedDeparture(String expectedDeparture) { this.expectedDeparture = expectedDeparture; }

    public String getActualArrival() { return actualArrival; }
    public void setActualArrival(String actualArrival) { this.actualArrival = actualArrival; }

    public String getActualDeparture() { return actualDeparture; }
    public void setActualDeparture(String actualDeparture) { this.actualDeparture = actualDeparture; }

    public boolean isArrived() { return isArrived; }
    public void setArrived(boolean arrived) { isArrived = arrived; }
}