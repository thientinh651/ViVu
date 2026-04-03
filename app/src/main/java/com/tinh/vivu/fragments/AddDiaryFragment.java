package com.tinh.vivu.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.JourneyLog;
import com.tinh.vivu.models.RouteStop;
import com.tinh.vivu.models.Trip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddDiaryFragment extends Fragment {

    private EditText edtTitle, edtContent;
    private Spinner spinnerTrip, spinnerRouteStop;
    private LinearLayout photoContainer;
    private View btnAddPhotoOption;
    private AppDatabase db;

    private List<Trip> tripList = new ArrayList<>();
    private List<RouteStop> routeStopList = new ArrayList<>();
    private List<String> selectedImagePaths = new ArrayList<>();
    private Uri cameraImageUri;

    private int existingLogId = -1;
    private JourneyLog existingLog;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) saveImageLocally(imageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (cameraImageUri != null) saveImageLocally(cameraImageUri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_diary, container, false);
        db = AppDatabase.getInstance(requireContext());

        edtTitle = view.findViewById(R.id.edt_diary_title);
        edtContent = view.findViewById(R.id.edt_diary_content);
        spinnerTrip = view.findViewById(R.id.spinner_trip);
        spinnerRouteStop = view.findViewById(R.id.spinner_route_stop);
        photoContainer = view.findViewById(R.id.photo_container);
        btnAddPhotoOption = view.findViewById(R.id.btn_add_photo_option);

        if (getArguments() != null) {
            existingLogId = getArguments().getInt("log_id", -1);
        }

        loadInitialData();

        if (btnAddPhotoOption != null) {
            btnAddPhotoOption.setOnClickListener(v -> {
                if (selectedImagePaths.size() >= 5) {
                    Toast.makeText(getContext(), "Tối đa 5 ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }
                showPhotoOptionsDialog();
            });
        }

        view.findViewById(R.id.btn_save_diary).setOnClickListener(v -> saveEntry());
        view.findViewById(R.id.btn_cancel_diary).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private void loadInitialData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Trip> trips = db.tripDao().getAllTrips();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    this.tripList = trips;
                    List<String> names = new ArrayList<>();
                    for (Trip t : trips) names.add(t.getName());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
                    spinnerTrip.setAdapter(adapter);

                    spinnerTrip.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            loadRouteStops(tripList.get(pos).getId());
                        }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });

                    if (existingLogId != -1) {
                        db.journeyLogDao().getAllLogs().observe(getViewLifecycleOwner(), logs -> {
                            for (JourneyLog log : logs) {
                                if (log.getId() == existingLogId) {
                                    existingLog = log;
                                    edtTitle.setText(log.getTitle());
                                    edtContent.setText(log.getContent());

                                    for (int i = 0; i < tripList.size(); i++) {
                                        if (tripList.get(i).getId() == log.getTripId()) {
                                            spinnerTrip.setSelection(i);
                                            break;
                                        }
                                    }

                                    // Xóa ảnh cũ nhưng giữ lại nút thêm ảnh
                                    photoContainer.removeAllViews();
                                    if (btnAddPhotoOption != null) {
                                        photoContainer.addView(btnAddPhotoOption);
                                    }

                                    selectedImagePaths.clear();
                                    if (log.getImagePaths() != null && !log.getImagePaths().isEmpty()) {
                                        String[] paths = log.getImagePaths().split("\\|");
                                        for (String path : paths) {
                                            selectedImagePaths.add(path);
                                            addThumbnailToView(path);
                                        }
                                    }
                                    break;
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void loadRouteStops(int tripId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<RouteStop> stops = db.routeStopDao().getStopsByTripId(tripId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    this.routeStopList = stops;
                    List<String> stopNames = new ArrayList<>();
                    stopNames.add("Không gắn điểm dừng cụ thể");
                    for (RouteStop s : stops) stopNames.add(s.getLocationName());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, stopNames);
                    spinnerRouteStop.setAdapter(adapter);

                    if (existingLog != null && existingLog.getPointId() != null) {
                        for (int i = 0; i < routeStopList.size(); i++) {
                            if (routeStopList.get(i).getId() == existingLog.getPointId()) {
                                spinnerRouteStop.setSelection(i + 1);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    private void showPhotoOptionsDialog() {
        String[] options = {"Chụp ảnh", "Thư viện"};
        new AlertDialog.Builder(getContext()).setItems(options, (dialog, which) -> {
            if (which == 0) openCamera();
            else openGallery();
        }).show();
    }

    private void openCamera() {
        try {
            File photoFile = new File(requireContext().getFilesDir(), "IMG_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void saveImageLocally(Uri uri) {
        try {
            String path = requireContext().getFilesDir() + "/DIARY_" + UUID.randomUUID() + ".jpg";
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            FileOutputStream fos = new FileOutputStream(path);
            byte[] buf = new byte[1024]; int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            fos.close(); is.close();

            selectedImagePaths.add(path);
            addThumbnailToView(path);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addThumbnailToView(String path) {
        if (getContext() == null) return;
        View item = LayoutInflater.from(getContext()).inflate(R.layout.item_photo_thumbnail, photoContainer, false);
        ImageView iv = item.findViewById(R.id.iv_thumbnail);
        ImageView btnRemove = item.findViewById(R.id.btn_remove_photo);

        Glide.with(requireContext()).load(path).centerCrop().into(iv);

        iv.setOnClickListener(v -> showFullImageDialog(path));

        btnRemove.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext()).setMessage("Bỏ hình ảnh này?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        selectedImagePaths.remove(path);
                        photoContainer.removeView(item);
                    }).setNegativeButton("Hủy", null)
                    .show();
        });

        // Thêm vào trước nút "Thêm ảnh" (btnAddPhotoOption luôn ở vị trí 0 sau khi clear và add lại)
        photoContainer.addView(item, 0);
    }

    private void showFullImageDialog(String path) {
        if (getActivity() == null) return;

        // Sử dụng Activity Context để tránh lỗi TokenException và đảm bảo tương thích theme
        final Dialog dialog = new Dialog(requireActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ImageView imageView = new ImageView(requireActivity());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setBackgroundColor(Color.BLACK);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(requireActivity()).load(path).into(imageView);

        dialog.setContentView(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveEntry() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) { Toast.makeText(getContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show(); return; }

        if (spinnerTrip.getSelectedItemPosition() < 0) return;
        Trip trip = tripList.get(spinnerTrip.getSelectedItemPosition());
        Integer pointId = null;
        String location = trip.getName();
        int stopPos = spinnerRouteStop.getSelectedItemPosition();
        if (stopPos > 0) {
            pointId = routeStopList.get(stopPos - 1).getId();
            location = routeStopList.get(stopPos - 1).getLocationName();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedImagePaths.size(); i++) {
            sb.append(selectedImagePaths.get(i)).append(i == selectedImagePaths.size() - 1 ? "" : "|");
        }

        if (existingLogId == -1) {
            JourneyLog log = new JourneyLog(trip.getId(), pointId, title, edtContent.getText().toString(), sb.toString(), location);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.journeyLogDao().insert(log);
                requireActivity().runOnUiThread(() -> getParentFragmentManager().popBackStack());
            });
        } else {
            existingLog.setTripId(trip.getId());
            existingLog.setPointId(pointId);
            existingLog.setTitle(title);
            existingLog.setContent(edtContent.getText().toString());
            existingLog.setImagePaths(sb.toString());
            existingLog.setLocationDisplay(location);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.journeyLogDao().update(existingLog);
                requireActivity().runOnUiThread(() -> getParentFragmentManager().popBackStack());
            });
        }
    }
}