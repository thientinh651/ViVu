package com.tinh.vivu.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.views.JourneyLogAdapter;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.JourneyLog;

import java.util.List;

public class DiaryFragment extends Fragment {

    private JourneyLogAdapter adapter;
    private AppDatabase db;
    private TextView tvEntries, tvPhotos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);
        db = AppDatabase.getInstance(requireContext());

        RecyclerView rv = view.findViewById(R.id.rv_diary);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new JourneyLogAdapter();
        rv.setAdapter(adapter);

        tvEntries = view.findViewById(R.id.tv_total_entries);
        tvPhotos = view.findViewById(R.id.tv_total_photos);

        db.journeyLogDao().getAllLogs().observe(getViewLifecycleOwner(), logs -> {
            adapter.setLogs(logs);
            updateStats(logs);
        });

        // XỬ LÝ CLICK ĐỂ XEM CHI TIẾT
        adapter.setOnLogClickListener(new JourneyLogAdapter.OnLogClickListener() {
            @Override
            public void onDeleteClick(JourneyLog log) {
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.diary_delete_confirm_message)
                        .setPositiveButton(R.string.common_delete, (d, w) -> {
                            AppDatabase.databaseWriteExecutor.execute(() -> db.journeyLogDao().delete(log));
                        }).setNegativeButton(R.string.common_cancel, null).show();
            }

            @Override
            public void onItemClick(JourneyLog log) {
                // Truyền ID sang AddDiaryFragment để mở chế độ Xem/Sửa
                AddDiaryFragment fragment = new AddDiaryFragment();
                Bundle args = new Bundle();
                args.putInt("log_id", log.getId());
                fragment.setArguments(args);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.fab_add_diary).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddDiaryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void updateStats(List<JourneyLog> logs) {
        if (tvEntries != null) tvEntries.setText(String.valueOf(logs.size()));
        int photoCount = 0;
        for (JourneyLog log : logs) {
            if (log.getImagePaths() != null && !log.getImagePaths().isEmpty()) {
                photoCount += log.getImagePaths().split("\\|").length;
            }
        }
        if (tvPhotos != null) tvPhotos.setText(String.valueOf(photoCount));
    }
}