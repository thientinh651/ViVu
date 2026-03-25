package com.tinh.vivu;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.views.TripAdapter; // Import thêm Adapter mới
import androidx.recyclerview.widget.LinearLayoutManager; // Cần import LayoutManager

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvTrips;
    private ImageView btnAddTrip;
    private AppDatabase database;
    private ExecutorService executorService;
    private List<Trip> tripList;

    // ĐÃ MỞ KHÓA ADAPTER
    private TripAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvTrips = findViewById(R.id.rv_trips);
        btnAddTrip = findViewById(R.id.btn_add_trip);

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        tripList = new ArrayList<>();

        // THIẾT LẬP RECYCLER VIEW
        adapter = new TripAdapter();
        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        rvTrips.setAdapter(adapter);

        // Bắt sự kiện xóa chuyến đi từ Adapter
        adapter.setOnTripClickListener(new TripAdapter.OnTripClickListener() {
            @Override
            public void onDeleteClick(Trip trip) {
                executorService.execute(() -> {
                    database.tripDao().delete(trip); // Bạn cần thêm @Delete void delete(Trip trip) vào TripDao.java nếu chưa có
                    loadTrips();
                });
            }

            @Override
            public void onTripClick(Trip trip) {
                // Tương lai sẽ chuyển sang màn hình Chi Tiết (Hình 3) ở đây
                Toast.makeText(MainActivity.this, "Mở chi tiết: " + trip.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Nút (+) mở Dialog tạo chuyến đi
        btnAddTrip.setOnClickListener(v -> showCreateTripDialog());

        loadTrips();
    }

    private void loadTrips() {
        executorService.execute(() -> {
            List<Trip> dbTrips = database.tripDao().getAllTrips();
            runOnUiThread(() -> {
                tripList.clear();
                tripList.addAll(dbTrips);

                // BƠM DỮ LIỆU VÀO ADAPTER
                adapter.setTrips(tripList);
            });
        });
    }

    private void showCreateTripDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_trip);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Cho Dialog rộng ra sát 2 viền màn hình
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        // Ánh xạ View trong Dialog
        EditText etTripName = dialog.findViewById(R.id.et_trip_name);
        EditText etStartDate = dialog.findViewById(R.id.et_start_date);
        EditText etEndDate = dialog.findViewById(R.id.et_end_date);
        EditText etBudget = dialog.findViewById(R.id.et_budget);
        MaterialButton btnCreate = dialog.findViewById(R.id.btn_create_trip);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        ImageView btnClose = dialog.findViewById(R.id.btn_close);

        // Nút đóng/hủy
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Nút Tạo mới
        btnCreate.setOnClickListener(v -> {
            String name = etTripName.getText().toString().trim();
            String startDate = etStartDate.getText().toString().trim();
            String endDate = etEndDate.getText().toString().trim();
            String budgetStr = etBudget.getText().toString().trim();

            if (name.isEmpty() || startDate.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Tên và Ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            double budget = budgetStr.isEmpty() ? 0 : Double.parseDouble(budgetStr);

            // Tạo đối tượng Trip (Chuẩn theo schema của bạn)
            Trip newTrip = new Trip(name, startDate, endDate, budget, "Đang lên kế hoạch");

            executorService.execute(() -> {
                database.tripDao().insert(newTrip); // Lưu vào DB
                loadTrips(); // Tải lại danh sách

                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Tạo chuyến đi thành công!", Toast.LENGTH_SHORT).show();

                    // Bước tiếp theo: Chúng ta sẽ dùng Intent để chuyển ID của chuyến đi này
                    // sang màn hình TripDetailActivity (Hình 3) ở đây!
                });
            });
        });

        dialog.show();
    }
}