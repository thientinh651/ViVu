package com.tinh.vivu;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
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
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.utils.ValidationUtils;
import com.tinh.vivu.views.TripAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvTrips;
    private ImageView btnAddTrip;
    private AppDatabase database;
    private ExecutorService executorService;
    private List<Trip> tripList;
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

        adapter = new TripAdapter();
        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        rvTrips.setAdapter(adapter);

        // Xử lý các sự kiện từ Danh sách (Xóa, Sửa, Click Chi tiết)
        adapter.setOnTripClickListener(new TripAdapter.OnTripClickListener() {
            @Override
            public void onDeleteClick(Trip trip) {
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa chuyến đi này không?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            executorService.execute(() -> {
                                database.tripDao().delete(trip);
                                loadTrips();
                            });
                        })
                        .setNegativeButton("Không", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            }


            @Override
            public void onEditClick(Trip trip) {
                // Sử dụng hàm setup chung để Sửa
                showTripDialog(trip);
            }

            @Override
            public void onTripClick(Trip trip) {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, TripDetailActivity.class);
                intent.putExtra("TRIP_ID", trip.getId());
                startActivity(intent);
            }
        });

        // Bấm dấu cộng để Tạo mới
        btnAddTrip.setOnClickListener(v -> showTripDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrips();
    }

    private void loadTrips() {
        executorService.execute(() -> {
            List<Trip> dbTrips = database.tripDao().getAllTrips();

            // Đợi dữ liệu mẫu nạp vào nếu lần đầu mở app
            if (dbTrips.isEmpty()) {
                try {
                    Thread.sleep(1000);
                    dbTrips = database.tripDao().getAllTrips();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            List<Trip> finalTrips = dbTrips;
            runOnUiThread(() -> {
                tripList.clear();
                tripList.addAll(finalTrips);
                adapter.setTrips(tripList);
            });
        });
    }

    /**
     * HÀM CHUNG ĐỂ XỬ LÝ CẢ TẠO MỚI VÀ CHỈNH SỬA
     * @param tripToEdit Nếu là null -> Tạo mới. Nếu có giá trị -> Chỉnh sửa.
     */
    private void showTripDialog(Trip tripToEdit) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_trip);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        // Ánh xạ View trong Dialog
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        EditText etName = dialog.findViewById(R.id.et_trip_name);
        EditText etStart = dialog.findViewById(R.id.et_start_date);
        EditText etEnd = dialog.findViewById(R.id.et_end_date);
        EditText etBudget = dialog.findViewById(R.id.et_budget);
        MaterialButton btnAction = dialog.findViewById(R.id.btn_create_trip);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        ImageView btnClose = dialog.findViewById(R.id.btn_close);

        // Đổ dữ liệu nếu là chế độ Sửa
        if (tripToEdit != null) {
            if (tvTitle != null) tvTitle.setText("Chỉnh Sửa Chuyến Đi");
            btnAction.setText("Cập nhật");
            etName.setText(tripToEdit.getName());
            etStart.setText(tripToEdit.getStartDate());
            etEnd.setText(tripToEdit.getEndDate());
            etBudget.setText(tripToEdit.getTotalBudget() == 0 ? "" : String.valueOf((long)tripToEdit.getTotalBudget()));
        }

        // Đóng dialog
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Chọn ngày (Sử dụng DatePickerDialog)
        etStart.setOnClickListener(v -> showDatePicker(etStart));
        etEnd.setOnClickListener(v -> showDatePicker(etEnd));

        // Xử lý khi bấm nút Xác nhận (Tạo/Cập nhật)
        btnAction.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String start = etStart.getText().toString().trim();
            String end = etEnd.getText().toString().trim();
            String budgetStr = etBudget.getText().toString().trim();

            // SỬ DỤNG VALIDATION UTILS ĐÃ TẠO
            if (ValidationUtils.isNullOrEmpty(name)) {
                Toast.makeText(this, "Vui lòng nhập tên chuyến đi!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ValidationUtils.isNullOrEmpty(start)) {
                Toast.makeText(this, "Vui lòng chọn ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra ngày: Start phải trước hoặc bằng End (nếu có nhập End)
            if (!ValidationUtils.isNullOrEmpty(end)) {
                if (!ValidationUtils.isStartBeforeOrEqualEnd(start, end, "dd/MM/yyyy")) {
                    Toast.makeText(this, "Ngày kết thúc không được trước ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double budget = ValidationUtils.isNumeric(budgetStr) ? Double.parseDouble(budgetStr) : 0;

            executorService.execute(() -> {
                if (tripToEdit == null) {
                    // Chế độ tạo mới
                    Trip newTrip = new Trip(name, start, end, budget, "Lên kế hoạch");
                    database.tripDao().insert(newTrip);
                } else {
                    // Chế độ chỉnh sửa
                    tripToEdit.setName(name);
                    tripToEdit.setStartDate(start);
                    tripToEdit.setEndDate(end);
                    tripToEdit.setTotalBudget(budget);
                    database.tripDao().update(tripToEdit);
                }

                loadTrips();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, tripToEdit == null ? "Tạo thành công!" : "Đã cập nhật!", Toast.LENGTH_SHORT).show();
                });
            });
        });

        dialog.show();
    }

    private void showDatePicker(EditText editText) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            editText.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
}