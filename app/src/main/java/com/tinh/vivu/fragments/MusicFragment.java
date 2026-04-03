package com.tinh.vivu.fragments;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.lifecycle.Observer;

import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.PlayList;
import com.tinh.vivu.models.Song;
import com.tinh.vivu.services.MusicService;
import com.tinh.vivu.views.PlaylistAdapter;
import com.tinh.vivu.views.SongAdapter;
import com.tinh.vivu.views.GroupedSongAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicFragment extends Fragment implements MusicService.MusicStateListener {

    private static final int REQUEST_PERMISSION = 123;
    private static final int REQUEST_PICK_MP3 = 200;
    
    private RecyclerView rvSongs, rvPlaylists, rvPlaylistDetailsSongs;
    private View layoutPlaylists, btnCreatePlaylist, layoutPlaylistDetails;
    private ImageView btnBackToPlaylists;
    private TextView tabSongs, tabPlaylists, tvPlaylistDetailsTitle;
    private TextView tvPlayerTitle, tvPlayerArtist, tvCurrentTime, tvTotalTime;
    private ImageView btnPlayPause, btnNext, btnPrev, btnShuffle, btnRepeat, btnAddPlaylist;
    private SeekBar seekBar;

    // Mini Player & Extras
    private View layoutMiniPlayer;
    private TextView tvMiniTitle, tvMiniArtist;
    private ImageView btnMiniPlayPause;
    private ProgressBar miniProgress;
    private LinearLayout sidebarAlphabet;
    private View btnPlayAll;

    private GroupedSongAdapter songAdapter;
    private SongAdapter playlistDetailsAdapter;
    private PlaylistAdapter playlistAdapter;

    private AppDatabase database;
    private ExecutorService executorService;
    
    private PlayList currentViewPlaylist = null;
    private List<Song> currentPlaylistSongs = new ArrayList<>();
    
    private MusicService musicService;
    private boolean isBound = false;
    private Intent playIntent;

    private List<Song> allSongs = new ArrayList<>();
    
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setListener(MusicFragment.this);
            isBound = true;
            if (!allSongs.isEmpty()) {
                musicService.setList(allSongs);
            }
            updatePlayerUI();
            handler.post(updateRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            handler.removeCallbacks(updateRunnable);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);

        rvSongs = view.findViewById(R.id.rv_songs);
        rvPlaylists = view.findViewById(R.id.rv_playlists);
        tabSongs = view.findViewById(R.id.tab_songs);
        tabPlaylists = view.findViewById(R.id.tab_playlists);
        
        tvPlayerTitle = view.findViewById(R.id.tv_player_title);
        tvPlayerArtist = view.findViewById(R.id.tv_player_artist);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnNext = view.findViewById(R.id.btn_next);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnShuffle = view.findViewById(R.id.btn_shuffle);
        btnRepeat = view.findViewById(R.id.btn_repeat);
        btnAddPlaylist = view.findViewById(R.id.btn_add_playlist);
        layoutPlaylists = view.findViewById(R.id.layout_playlists);
        btnCreatePlaylist = view.findViewById(R.id.btn_create_playlist);
        
        layoutPlaylistDetails = view.findViewById(R.id.layout_playlist_details);
        btnBackToPlaylists = view.findViewById(R.id.btn_back_to_playlists);
        tvPlaylistDetailsTitle = view.findViewById(R.id.tv_playlist_details_title);
        rvPlaylistDetailsSongs = view.findViewById(R.id.rv_playlist_details_songs);
        
        seekBar = view.findViewById(R.id.seek_bar);

        layoutMiniPlayer = view.findViewById(R.id.layout_mini_player);
        tvMiniTitle = view.findViewById(R.id.tv_mini_title);
        tvMiniArtist = view.findViewById(R.id.tv_mini_artist);
        btnMiniPlayPause = view.findViewById(R.id.btn_mini_play_pause);
        miniProgress = view.findViewById(R.id.mini_progress);
        sidebarAlphabet = view.findViewById(R.id.sidebar_alphabet);
        btnPlayAll = view.findViewById(R.id.btn_play_all);

        setupAlphabetSidebar();

        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        setupAdapters();
        setupListeners();
        setupUpdateRunnable();
        checkPermissionsAndLoadSongs();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(requireContext(), MusicService.class);
            requireActivity().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(playIntent);
            } else {
                requireActivity().startService(playIntent);
            }
        }
    }

    private void setupAdapters() {
        songAdapter = new GroupedSongAdapter();
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(songAdapter);

        rvSongs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisible != RecyclerView.NO_POSITION) {
                        String letter = songAdapter.getLetterForPosition(firstVisible);
                        highlightSidebarLetter(letter);
                    }
                }
            }
        });

        playlistAdapter = new PlaylistAdapter();
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaylists.setAdapter(playlistAdapter);

        ItemTouchHelper playlistTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                PlayList playList = playlistAdapter.getPlaylist(position);
                if (playList == null || playList.getId() == -1) {
                    playlistAdapter.notifyItemChanged(position);
                    return;
                }
                if (direction == ItemTouchHelper.RIGHT) {
                    new AlertDialog.Builder(requireContext())
                        .setMessage("Bạn có chắc muốn xóa Playlist '" + playList.getName() + "' không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            executorService.execute(() -> database.playListDao().delete(playList));
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Đã xóa", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> playlistAdapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> playlistAdapter.notifyItemChanged(position))
                        .show();
                } else if (direction == ItemTouchHelper.LEFT) {
                    EditText input = new EditText(requireContext());
                    input.setText(playList.getName());
                    input.setPadding(40, 40, 40, 40);
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Đổi tên Playlist")
                        .setView(input)
                        .setPositiveButton("Lưu", (dialog, which) -> {
                            String newName = input.getText().toString().trim();
                            if (!newName.isEmpty()) {
                                playList.setName(newName);
                                executorService.execute(() -> database.playListDao().update(playList));
                            }
                            playlistAdapter.notifyItemChanged(position);
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> playlistAdapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> playlistAdapter.notifyItemChanged(position))
                        .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                PlayList pl = playlistAdapter.getPlaylist(viewHolder.getAdapterPosition());
                if (pl != null && pl.getId() == -1) return;

                View itemView = viewHolder.itemView;
                android.graphics.drawable.ColorDrawable background = new android.graphics.drawable.ColorDrawable();
                android.graphics.drawable.Drawable icon;

                if (dX > 0) {
                    background.setColor(0xFFF44336);
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                } else if (dX < 0) {
                    background.setColor(0xFF4CAF50);
                    background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_edit);
                } else {
                    background.setBounds(0,0,0,0);
                    icon = null;
                }
                background.draw(c);
                if (icon != null) {
                    int margin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int top = itemView.getTop() + margin;
                    int bottom = top + icon.getIntrinsicHeight();
                    if (dX > 0) {
                        icon.setBounds(itemView.getLeft() + margin, top, itemView.getLeft() + margin + icon.getIntrinsicWidth(), bottom);
                    } else if (dX < 0) {
                        icon.setBounds(itemView.getRight() - margin - icon.getIntrinsicWidth(), top, itemView.getRight() - margin, bottom);
                    }
                    icon.draw(c);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        playlistTouchHelper.attachToRecyclerView(rvPlaylists);

        playlistDetailsAdapter = new SongAdapter();
        rvPlaylistDetailsSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaylistDetailsSongs.setAdapter(playlistDetailsAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                
                Song movedSong = currentPlaylistSongs.remove(from);
                currentPlaylistSongs.add(to, movedSong);
                playlistDetailsAdapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Song removedSong = currentPlaylistSongs.get(position);
                
                if (currentViewPlaylist != null) {
                    executorService.execute(() -> {
                        database.playListSongDao().removeSongFromPlayList(currentViewPlaylist.getId(), removedSong.getId());
                    });
                }
                
                currentPlaylistSongs.remove(position);
                playlistDetailsAdapter.notifyItemRemoved(position);
                Toast.makeText(requireContext(), "Đã xóa " + removedSong.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (currentViewPlaylist != null) {
                    executorService.execute(() -> {
                        for (int i = 0; i < currentPlaylistSongs.size(); i++) {
                            database.playListSongDao().updateOrderIndex(currentViewPlaylist.getId(), currentPlaylistSongs.get(i).getId(), i);
                        }
                    });
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(rvPlaylistDetailsSongs);
    }

    private void setupListeners() {
        tabSongs.setOnClickListener(v -> {
            layoutPlaylistDetails.setVisibility(View.GONE);
            rvSongs.setVisibility(View.VISIBLE);
            layoutPlaylists.setVisibility(View.GONE);
            tabSongs.setTextColor(0xFF8E24AA);
            tabPlaylists.setTextColor(0xFF888888);
        });

        tabPlaylists.setOnClickListener(v -> {
            layoutPlaylistDetails.setVisibility(View.GONE);
            rvSongs.setVisibility(View.GONE);
            layoutPlaylists.setVisibility(View.VISIBLE);
            tabPlaylists.setTextColor(0xFF8E24AA);
            tabSongs.setTextColor(0xFF888888);
        });

        btnBackToPlaylists.setOnClickListener(v -> {
            currentViewPlaylist = null;
            layoutPlaylistDetails.setVisibility(View.GONE);
            layoutPlaylists.setVisibility(View.VISIBLE);
        });

        btnPlayPause.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.togglePlayPause();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isBound && musicService != null) musicService.playNext();
        });

        btnPrev.setOnClickListener(v -> {
            if (isBound && musicService != null) musicService.playPrev();
        });

        btnShuffle.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                boolean shuffle = !musicService.isShuffle();
                musicService.setShuffle(shuffle);
                btnShuffle.setAlpha(shuffle ? 1.0f : 0.5f);
            }
        });

        btnRepeat.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                boolean repeat = !musicService.isRepeat();
                musicService.setRepeat(repeat);
                btnRepeat.setAlpha(repeat ? 1.0f : 0.5f);
            }
        });

        btnAddPlaylist.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("audio/mpeg");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "Chọn file MP3"), REQUEST_PICK_MP3);
        });

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        btnPlayAll.setOnClickListener(v -> {
            if (isBound && musicService != null && currentPlaylistSongs != null && !currentPlaylistSongs.isEmpty()) {
                musicService.setList(currentPlaylistSongs);
                musicService.playSong(0);
            } else if (currentPlaylistSongs == null || currentPlaylistSongs.isEmpty()) {
                Toast.makeText(requireContext(), "Chưa có bài hát nào trong danh sách phát này", Toast.LENGTH_SHORT).show();
            }
        });

        btnMiniPlayPause.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.togglePlayPause();
            }
        });

        layoutMiniPlayer.setOnClickListener(v -> {
            try {
                View bottomSheet = requireView().findViewById(R.id.bottom_sheet);
                if (bottomSheet != null) {
                    com.google.android.material.bottomsheet.BottomSheetBehavior behavior = 
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                    behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
                }
            } catch (Exception e) {}
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && musicService != null) {
                    musicService.seek(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        songAdapter.setOnSongClickListener(new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(Song song, int position) {
                if (isBound && musicService != null) {
                    musicService.setList(allSongs);
                    musicService.playSong(position);
                }
            }

            @Override
            public void onFavoriteClick(Song song, int position) {
                boolean isFav = !song.isFavorite();
                song.setFavorite(isFav);
                songAdapter.notifyDataSetChanged();
                executorService.execute(() -> database.songDao().updateFavoriteStatus(song.getId(), isFav));
            }

            @Override
            public void onSongLongClick(View v, Song song, int position) {
                showAddToPlaylistDialog(song);
            }
        });

        playlistDetailsAdapter.setOnSongClickListener(new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(Song song, int position) {
                if (isBound && musicService != null) {
                    musicService.setList(currentPlaylistSongs);
                    musicService.playSong(position);
                }
            }

            @Override
            public void onFavoriteClick(Song song, int position) {
                boolean isFav = !song.isFavorite();
                song.setFavorite(isFav);
                playlistDetailsAdapter.notifyItemChanged(position);
                executorService.execute(() -> database.songDao().updateFavoriteStatus(song.getId(), isFav));
            }

            @Override
            public void onSongLongClick(View v, Song song, int position) {
                // Disabled. Use standard ItemTouchHelper swipe-to-delete now.
            }
        });

        playlistAdapter.setOnPlaylistClickListener(new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(PlayList playList) {
                openPlaylistDetails(playList);
            }
        });
    }

    private void openPlaylistDetails(PlayList playList) {
        currentViewPlaylist = playList;
        tvPlaylistDetailsTitle.setText(playList.getName());
        layoutPlaylists.setVisibility(View.GONE);
        rvSongs.setVisibility(View.GONE);
        layoutPlaylistDetails.setVisibility(View.VISIBLE);

        if (playList.getId() == -1) {
            database.songDao().getAllSongs().observe(getViewLifecycleOwner(), songs -> {
                if (currentViewPlaylist != null && currentViewPlaylist.getId() == -1) {
                    currentPlaylistSongs = new ArrayList<>();
                    if (songs != null) {
                        for (Song s : songs) {
                            if (s.isFavorite()) currentPlaylistSongs.add(s);
                        }
                    }
                    playlistDetailsAdapter.setSongs(currentPlaylistSongs);
                }
            });
        } else {
            database.playListSongDao().getSongsForPlayListLiveData(playList.getId()).observe(getViewLifecycleOwner(), songs -> {
                if (currentViewPlaylist != null && currentViewPlaylist.getId() == playList.getId()) {
                    currentPlaylistSongs = new ArrayList<>(songs);
                    playlistDetailsAdapter.setSongs(currentPlaylistSongs);
                }
            });
        }
    }

    private void setupAlphabetSidebar() {
        sidebarAlphabet.removeAllViews();
        String[] alphabet = {"#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
        for (String letter : alphabet) {
            TextView tv = new TextView(requireContext());
            tv.setText(letter);
            tv.setTextSize(10f);
            tv.setTextColor(0xFF888888);
            tv.setPadding(4, 2, 4, 2);
            tv.setGravity(android.view.Gravity.CENTER);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            tv.setLayoutParams(params);
            
            tv.setOnClickListener(v -> scrollToLetter(letter));
            sidebarAlphabet.addView(tv);
        }
    }

    private void highlightSidebarLetter(String activeLetter) {
        for (int i = 0; i < sidebarAlphabet.getChildCount(); i++) {
            TextView tv = (TextView) sidebarAlphabet.getChildAt(i);
            if (activeLetter != null && activeLetter.equals(tv.getText().toString())) {
                tv.setBackgroundResource(R.drawable.bg_circle_active);
                tv.setTextColor(0xFF8E24AA);
                tv.setTextSize(12f);
            } else {
                tv.setBackgroundResource(0);
                tv.setTextColor(0xFF888888);
                tv.setTextSize(10f);
            }
        }
    }

    private void scrollToLetter(String letter) {
        if (allSongs == null || allSongs.isEmpty()) return;
        int pos = songAdapter.getPositionForLetter(letter);
        if (pos != -1) {
            ((LinearLayoutManager)rvSongs.getLayoutManager()).scrollToPositionWithOffset(pos, 0);
        }
    }

    private void showAddToPlaylistDialog(Song song) {
        database.playListDao().getAllPlayLists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists == null || playlists.isEmpty()) {
                Toast.makeText(requireContext(), "Chưa có danh sách phát nào", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[playlists.size()];
            for(int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).getName();
            
            new AlertDialog.Builder(requireContext())
                .setTitle("Thêm vào danh sách phát")
                .setItems(names, (dialog, which) -> {
                    PlayList selected = playlists.get(which);
                    executorService.execute(() -> {
                        int maxOrder = database.playListSongDao().getMaxOrderIndex(selected.getId());
                        database.playListSongDao().insert(new com.tinh.vivu.models.PlayListSong(selected.getId(), song.getId(), maxOrder + 1));
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Đã thêm vào " + selected.getName(), Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .show();
        });
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(" Tên danh sách phát mới...");
        input.setPadding(40, 40, 40, 40);
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Tạo Playlist mới")
            .setView(input)
            .setPositiveButton("Tạo", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    executorService.execute(() -> {
                        database.playListDao().insert(new PlayList(name));
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Đã tạo: " + name, Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void setupUpdateRunnable() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound && musicService != null && musicService.isPlaying()) {
                    int pos = musicService.getPosition();
                    seekBar.setProgress(pos);
                    tvCurrentTime.setText(formatTime(pos));
                    int dur = musicService.getDuration();
                    if (dur > 0) {
                        miniProgress.setProgress((int) ((pos * 100f) / dur));
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    private void checkPermissionsAndLoadSongs() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
            Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, REQUEST_PERMISSION);
        } else {
            loadSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        } else {
            Toast.makeText(requireContext(), "Cần cấp quyền truy cập để đọc bài hát", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSongs() {
        database.songDao().getAllSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs == null || songs.isEmpty()) {
                scanDeviceSongs();
            } else {
                allSongs = songs;
                songAdapter.setSongs(allSongs);
                if (isBound && musicService != null) {
                    musicService.setList(allSongs);
                    updatePlayerUI();
                }
            }
        });
        
        database.playListDao().getAllPlayLists().observe(getViewLifecycleOwner(), playlists -> {
            List<PlayList> withFav = new ArrayList<>();
            PlayList favPlaylist = new PlayList("Favourite");
            favPlaylist.setId(-1); // Special ID
            withFav.add(favPlaylist);
            if (playlists != null) withFav.addAll(playlists);
            playlistAdapter.setPlaylists(withFav);
        });
    }

    private String fixEncoding(String text) {
        return text;
    }

    private void scanDeviceSongs() {
        executorService.execute(() -> {
            Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String[] projection = new String[]{
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            
            try (Cursor cursor = requireContext().getContentResolver().query(
                    collection, projection, selection, null, MediaStore.Audio.Media.TITLE + " ASC")) {
                
                if (cursor != null) {
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                    while (cursor.moveToNext()) {
                        String title = fixEncoding(cursor.getString(titleCol));
                        String artist = fixEncoding(cursor.getString(artistCol));
                        String path = cursor.getString(dataCol);
                        long duration = cursor.getLong(durationCol);

                        Song song = new Song(title, artist, path, duration);
                        database.songDao().insert(song);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updatePlayerUI() {
        if (isBound && musicService != null) {
            Song curr = musicService.getCurrentSong();
            if (curr != null) {
                tvPlayerTitle.setText(curr.getTitle());
                tvPlayerArtist.setText(curr.getArtist());

                tvMiniTitle.setText(curr.getTitle());
                tvMiniArtist.setText(curr.getArtist());
                layoutMiniPlayer.setVisibility(View.VISIBLE);
                
                int dur = musicService.getDuration();
                seekBar.setMax(dur);
                tvTotalTime.setText(formatTime(dur));
            } else {
                layoutMiniPlayer.setVisibility(View.GONE);
            }

            if (musicService.isPlaying()) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                btnMiniPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                btnMiniPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
            
            btnShuffle.setAlpha(musicService.isShuffle() ? 1.0f : 0.5f);
            btnRepeat.setAlpha(musicService.isRepeat() ? 1.0f : 0.5f);
        }
    }

    @Override
    public void onStateChanged() {
        requireActivity().runOnUiThread(this::updatePlayerUI);
    }

    @Override
    public void onSongCompleted() {
        requireActivity().runOnUiThread(() -> {
            seekBar.setProgress(0);
            tvCurrentTime.setText("0:00");
            updatePlayerUI();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        if (isBound && playIntent != null) {
            requireActivity().unbindService(musicConnection);
            isBound = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_MP3 && resultCode == requireActivity().RESULT_OK && data != null) {
            Uri audioUri = data.getData();
            if (audioUri != null) {
                // Fetch details and add to DB
                executorService.execute(() -> {
                    try (Cursor cursor = requireContext().getContentResolver().query(audioUri, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                            int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                            int durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                            
                            String title = titleCol != -1 ? fixEncoding(cursor.getString(titleCol)) : "Unknown Title";
                            String artist = artistCol != -1 ? fixEncoding(cursor.getString(artistCol)) : "Unknown Artist";
                            long duration = durCol != -1 ? cursor.getLong(durCol) : 0;
                            
                            Song newSong = new Song(title, artist, audioUri.toString(), duration);
                            database.songDao().insert(newSong);
                            
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Đã thêm bài hát!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
