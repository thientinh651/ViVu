package com.tinh.vivu.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.Song;

import java.util.List;

@Dao
public interface SongDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Song song);
    
    @Update
    void update(Song song);

    @Delete
    void delete(Song song);

    @Query("SELECT * FROM songs")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    LiveData<List<Song>> getFavoriteSongs();

    @Query("SELECT * FROM songs WHERE filePath = :path LIMIT 1")
    Song getSongByPath(String path);

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    void updateFavoriteStatus(int songId, boolean isFavorite);
}
