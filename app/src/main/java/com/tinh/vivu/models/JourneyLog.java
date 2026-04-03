package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "journey_logs")
public class JourneyLog {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int tripId;
    private Integer pointId; // Có thể null nếu không gắn với điểm dừng
    private String title;
    private String content;
    private String imagePaths; // Lưu danh sách đường dẫn ảnh, phân tách bởi dấu "|"
    private String locationDisplay; // Tên địa điểm hiển thị
    private long createdAt;

    public JourneyLog(int tripId, Integer pointId, String title, String content, String imagePaths, String locationDisplay) {
        this.tripId = tripId;
        this.pointId = pointId;
        this.title = title;
        this.content = content;
        this.imagePaths = imagePaths;
        this.locationDisplay = locationDisplay;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }
    public Integer getPointId() { return pointId; }
    public void setPointId(Integer pointId) { this.pointId = pointId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImagePaths() { return imagePaths; }
    public void setImagePaths(String imagePaths) { this.imagePaths = imagePaths; }
    public String getLocationDisplay() { return locationDisplay; }
    public void setLocationDisplay(String locationDisplay) { this.locationDisplay = locationDisplay; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}