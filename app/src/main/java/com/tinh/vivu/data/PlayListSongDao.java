package com.tinh.vivu.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.tinh.vivu.models.PlayListSong;
import com.tinh.vivu.models.Song;

import java.util.List;

@Dao
public interface PlayListSongDao {
    @Insert
    void insert(PlayListSong playListSong);

    @Query("UPDATE playlist_songs SET orderIndex = :newOrderIndex WHERE playListId = :playListId AND songId = :songId")
    void updateOrderIndex(int playListId, int songId, int newOrderIndex);

    @Query("SELECT COALESCE(MAX(orderIndex), 0) FROM playlist_songs WHERE playListId = :playListId")
    int getMaxOrderIndex(int playListId);

    @Delete
    void delete(PlayListSong playListSong);

    @Query("DELETE FROM playlist_songs WHERE playListId = :playListId AND songId = :songId")
    void removeSongFromPlayList(int playListId, int songId);

    @Query("SELECT songs.* FROM songs INNER JOIN playlist_songs ON songs.id = playlist_songs.songId WHERE playlist_songs.playListId = :playListId ORDER BY playlist_songs.orderIndex ASC")
    LiveData<List<Song>> getSongsForPlayListLiveData(int playListId);

    @Query("SELECT songs.* FROM songs INNER JOIN playlist_songs ON songs.id = playlist_songs.songId WHERE playlist_songs.playListId = :playListId ORDER BY playlist_songs.orderIndex ASC")
    List<Song> getSongsForPlayList(int playListId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playListId = :playListId AND songId = :songId")
    int checkSongInPlayList(int playListId, int songId);
}
