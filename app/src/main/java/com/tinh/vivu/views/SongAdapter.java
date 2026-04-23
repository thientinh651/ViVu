package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.Song;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList = new ArrayList<>();
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
        void onFavoriteClick(Song song, int position);
        void onSongLongClick(View v, Song song, int position);
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs) {
        this.songList = songs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        
        if (song.isFavorite()) {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
        }
    }

    @Override
    public int getItemCount() {
        return songList == null ? 0 : songList.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvArtist;
        ImageView btnFavorite;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_song_title);
            tvArtist = itemView.findViewById(R.id.tv_song_artist);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onSongClick(songList.get(position), position);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onFavoriteClick(songList.get(position), position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onSongLongClick(v, songList.get(position), position);
                    return true;
                }
                return false;
            });
        }
    }
}
