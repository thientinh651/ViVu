package com.tinh.vivu;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.RouteStop;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.views.RouteStopAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripDetailActivity extends AppCompatActivity {

    private TextView tvTripName, tvDate;
    private ImageView btnBack;
    private RecyclerView rvStops;
    private MaterialButton btnAddStop;

    // 3 Tab Trạng thái
    private TextView tabPlanning, tabOngoing, tabCompleted;

    private AppDatabase database;
    private ExecutorService executorService;
    private RouteStopAdapter adapter;
    private List<RouteStop> stopList;

    private int currentTripId = -1;
    private Trip currentTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        // 1. Ánh xạ View
        tvTripName = findViewById(R.id.tv_detail_trip_name);
        tvDate = findViewById(R.id.tv_detail_date);
        btnBack = findViewById(R.id.btn_back);
        rvStops = findViewById(R.id.rv_detail_stops);
        btnAddStop = findViewById(R.id.btn_add_route_stop);

        tabPlanning = findViewById(R.id.tab_planning);
        tabOngoing = findViewById(R.id.tab_ongoing);
        tabCompleted = findViewById(R.id.tab_completed);

        // 2. Khởi tạo Database & Thread
        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        stopList = new ArrayList<>();

        // 3. Cấu hình RecyclerView
        adapter = new RouteStopAdapter();
        rvStops.setLayoutManager(new LinearLayoutManager(this));
        rvStops.setAdapter(adapter);

        // 4. Lắng nghe sự kiện
        btnBack.setOnClickListener(v -> finish());
        btnAddStop.setOnClickListener(v -> showAddStopDialog());

        // Lắng nghe sự kiện từ Adapter (Xóa hoặc Tick Checkbox)
        adapter.setOnStopClickListener(new RouteStopAdapter.OnStopClickListener() {
            @Override
            public void onDeleteClick(RouteStop stop) {
                new androidx.appcompat.app.AlertDialog.Builder(TripDetailActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa điểm dừng này không?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            executorService.execute(() -> {
                                // Bước 1: Xóa điểm dừng hiện tại
                                database.routeStopDao().delete(stop);

                                // Bước 2: Lấy lại danh sách các điểm còn lại của chuyến đi này
                                List<RouteStop> remainingStops = database.routeStopDao().getStopsByTripId(currentTripId);

                                // Bước 3: Đánh số lại thứ tự (orderIndex) cho các điểm còn lại từ 1 trở đi
                                for (int i = 0; i < remainingStops.size(); i++) {
                                    RouteStop s = remainingStops.get(i);
                                    s.setOrderIndex(i + 1); // Đánh số lại 1, 2, 3...
                                    database.routeStopDao().update(s); // Cập nhật vào Database
                                }

                                // Bước 4: Tải lại danh sách lên giao diện
                                loadRouteStops();
                            });
                        })
                        .setNegativeButton("Không", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            }


            @Override
            public void onUpdateClick(RouteStop stop) {
                // Lưu lại ngay khi người dùng tick Checkbox (Đã đến/Đã rời)
                executorService.execute(() -> {
                    database.routeStopDao().update(stop);
                });
            }
        });

        // Xử lý click chọn Tab trạng thái
        tabPlanning.setOnClickListener(v -> updateTripStatus("Lên kế hoạch"));
        tabOngoing.setOnClickListener(v -> updateTripStatus("Đang đi"));
        tabCompleted.setOnClickListener(v -> updateTripStatus("Hoàn thành"));

        // 5. Lấy dữ liệu ID từ Intent
        currentTripId = getIntent().getIntExtra("TRIP_ID", -1);
        if (currentTripId != -1) {
            loadTripDetails();
            loadRouteStops();
        }
    }

    // Cập nhật trạng thái chuyến đi
    private void updateTripStatus(String newStatus) {
        if (currentTrip != null) {
            currentTrip.setStatus(newStatus);
            updateTabUI(newStatus);
            executorService.execute(() -> {
                database.tripDao().update(currentTrip);
            });
        }
    }

    // Đổi màu nền trắng cho Tab được chọn
    private void updateTabUI(String status) {
        tabPlanning.setBackgroundResource(0);
        tabPlanning.setTextColor(Color.WHITE);
        tabOngoing.setBackgroundResource(0);
        tabOngoing.setTextColor(Color.WHITE);
        tabCompleted.setBackgroundResource(0);
        tabCompleted.setTextColor(Color.WHITE);

        int tealColor = Color.parseColor("#00897B");

        if ("Đang đi".equals(status)) {
            tabOngoing.setBackgroundResource(R.drawable.bg_tab_active);
            tabOngoing.setTextColor(tealColor);
        } else if ("Hoàn thành".equals(status)) {
            tabCompleted.setBackgroundResource(R.drawable.bg_tab_active);
            tabCompleted.setTextColor(tealColor);
        } else {
            tabPlanning.setBackgroundResource(R.drawable.bg_tab_active);
            tabPlanning.setTextColor(tealColor);
        }
    }

    // Hiển thị Dialog thêm điểm dừng
    private void showAddStopDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_stop);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        EditText etName = dialog.findViewById(R.id.et_location_name);
        EditText etArr = dialog.findViewById(R.id.et_expected_arrival);
        EditText etDep = dialog.findViewById(R.id.et_expected_departure);
        MaterialButton btnAdd = dialog.findViewById(R.id.btn_add);

        etArr.setOnClickListener(v -> pickDateTime(etArr));
        etDep.setOnClickListener(v -> pickDateTime(etDep));

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String arr = etArr.getText().toString().trim();
            String dep = etDep.getText().toString().trim();

            if (name.isEmpty() || arr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            int index = stopList.size() + 1;
            RouteStop stop = new RouteStop(currentTripId, name, index, arr, dep);

            executorService.execute(() -> {
                database.routeStopDao().insert(stop);
                loadRouteStops();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Đã thêm điểm dừng!", Toast.LENGTH_SHORT).show();
                });
            });
        });

        dialog.show();
    }

    // Hàm chọn Ngày rồi chọn Giờ
    private void pickDateTime(EditText et) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            new TimePickerDialog(this, (view1, h, min) -> {
                et.setText(String.format("%02d:%02d %02d/%02d", h, min, d, m + 1));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadTripDetails() {
        executorService.execute(() -> {
            currentTrip = database.tripDao().getTripById(currentTripId);
            if (currentTrip != null) {
                runOnUiThread(() -> {
                    tvTripName.setText(currentTrip.getName());
                    tvDate.setText(currentTrip.getStartDate() + " - " + currentTrip.getEndDate());
                    updateTabUI(currentTrip.getStatus());
                });
            }
        });
    }

    private void loadRouteStops() {
        executorService.execute(() -> {
            List<RouteStop> dbStops = database.routeStopDao().getStopsByTripId(currentTripId);
            runOnUiThread(() -> {
                stopList.clear();
                stopList.addAll(dbStops);
                adapter.setStops(stopList);
            });
        });
    }
}