package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlayList {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;

    public PlayList(String name) {
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
