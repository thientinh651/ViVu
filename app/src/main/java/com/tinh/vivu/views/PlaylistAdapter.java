package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.PlayList;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<PlayList> playlistList = new ArrayList<>();
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(PlayList playList);
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<PlayList> playlists) {
        this.playlistList = playlists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlayList playlist = playlistList.get(position);
        holder.tvName.setText(playlist.getName());
    }

    @Override
    public int getItemCount() {
        return playlistList == null ? 0 : playlistList.size();
    }

    public PlayList getPlaylist(int position) {
        if (playlistList != null && position >= 0 && position < playlistList.size()) {
            return playlistList.get(position);
        }
        return null;
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_playlist_name);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onPlaylistClick(playlistList.get(position));
                }
            });
        }
    }
}
