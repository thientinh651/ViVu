package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String artist;
    private String filePath;
    private long duration;
    private boolean isFavorite;

    public Song(String title, String artist, String filePath, long duration) {
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.duration = duration;
        this.isFavorite = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}
