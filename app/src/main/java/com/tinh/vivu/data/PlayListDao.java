package com.tinh.vivu.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.PlayList;

import java.util.List;

@Dao
public interface PlayListDao {
    @Insert
    long insert(PlayList playList);

    @Update
    void update(PlayList playList);

    @Delete
    void delete(PlayList playList);

    @Query("SELECT * FROM playlists")
    LiveData<List<PlayList>> getAllPlayLists();

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    PlayList getPlayListById(int id);
}
