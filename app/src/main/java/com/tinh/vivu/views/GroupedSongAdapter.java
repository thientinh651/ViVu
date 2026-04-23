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

public class GroupedSongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<SongAdapter.OnSongClickListener> listeners = new ArrayList<>();
    private SongAdapter.OnSongClickListener listener;

    public void setOnSongClickListener(SongAdapter.OnSongClickListener listener) {
        this.listener = listener;
    }

    public static class SongItem {
        public boolean isHeader;
        public String letter;
        public Song song;
        public int originalIndex;

        public SongItem(String letter) {
            this.isHeader = true;
            this.letter = letter;
        }

        public SongItem(Song song, int originalIndex) {
            this.isHeader = false;
            this.song = song;
            this.originalIndex = originalIndex;
        }
    }

    private List<SongItem> itemList = new ArrayList<>();

    public void setSongs(List<Song> songs) {
        itemList.clear();
        if (songs != null) {
            String lastLetter = "";
            for (int i = 0; i < songs.size(); i++) {
                Song s = songs.get(i);
                if (s.getTitle() != null && !s.getTitle().isEmpty()) {
                    String firstChar = s.getTitle().substring(0, 1).toUpperCase();
                    if (!Character.isLetter(firstChar.charAt(0))) {
                        firstChar = "#";
                    }
                    if (!firstChar.equals(lastLetter)) {
                        lastLetter = firstChar;
                        itemList.add(new SongItem(lastLetter));
                    }
                }
                itemList.add(new SongItem(s, i));
            }
        }
        notifyDataSetChanged();
    }

    public String getLetterForPosition(int position) {
        if (position >= 0 && position < itemList.size()) {
            SongItem item = itemList.get(position);
            if (item.isHeader) return item.letter;
            if (item.song != null && item.song.getTitle() != null && !item.song.getTitle().isEmpty()) {
                String letter = item.song.getTitle().substring(0, 1).toUpperCase();
                return Character.isLetter(letter.charAt(0)) ? letter : "#";
            }
        }
        return "#";
    }

    public int getPositionForLetter(String letter) {
        for (int i = 0; i < itemList.size(); i++) {
            SongItem item = itemList.get(i);
            if (item.isHeader && item.letter.equals(letter)) {
                return i;
            } else if (!item.isHeader && item.song != null && item.song.getTitle() != null && !item.song.getTitle().isEmpty()) {
                String firstChar = item.song.getTitle().substring(0, 1).toUpperCase();
                if (!Character.isLetter(firstChar.charAt(0))) firstChar = "#";
                if (firstChar.equals(letter)) return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
            return new SongViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SongItem item = itemList.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvLetter.setText(item.letter);
        } else if (holder instanceof SongViewHolder) {
            SongViewHolder h = (SongViewHolder) holder;
            Song song = item.song;
            h.tvTitle.setText(song.getTitle());
            h.tvArtist.setText(song.getArtist());
            if (song.isFavorite()) {
                h.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            } else {
                h.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvLetter;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLetter = itemView.findViewById(R.id.tv_header_letter);
        }
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
                    listener.onSongClick(itemList.get(position).song, itemList.get(position).originalIndex);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onFavoriteClick(itemList.get(position).song, itemList.get(position).originalIndex);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onSongLongClick(v, itemList.get(position).song, itemList.get(position).originalIndex);
                    return true;
                }
                return false;
            });
        }
    }
}
