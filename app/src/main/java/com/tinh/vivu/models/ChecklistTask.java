package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "checklist_tasks")
public class ChecklistTask {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int categoryId;
    private String name;
    private boolean isCompleted;
    private long completedAt;
    private long createdAt;

    public ChecklistTask(int categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}