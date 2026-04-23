package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "playlist_songs",
        primaryKeys = {"playListId", "songId"},
        indices = {@Index("songId")},
        foreignKeys = {
            @ForeignKey(entity = PlayList.class,
                        parentColumns = "id",
                        childColumns = "playListId",
                        onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = ForeignKey.CASCADE)
        })
public class PlayListSong {
    private int playListId;
    private int songId;
    private int orderIndex;

    public PlayListSong(int playListId, int songId, int orderIndex) {
        this.playListId = playListId;
        this.songId = songId;
        this.orderIndex = orderIndex;
    }

    public int getPlayListId() { return playListId; }
    public void setPlayListId(int playListId) { this.playListId = playListId; }

    public int getSongId() { return songId; }
    public void setSongId(int songId) { this.songId = songId; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
