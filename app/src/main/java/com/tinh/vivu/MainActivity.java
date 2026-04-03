//package com.tinh.vivu;
//
//import android.app.DatePickerDialog;
//import android.app.Dialog;
//import android.content.Intent;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.os.Bundle;
//import android.view.Window;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.material.bottomnavigation.BottomNavigationView;
//import com.google.android.material.button.MaterialButton;
//import com.tinh.vivu.data.AppDatabase;
//import com.tinh.vivu.models.Trip;
//import com.tinh.vivu.utils.ValidationUtils;
//import com.tinh.vivu.views.TripAdapter;
//
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MainActivity extends AppCompatActivity {
//
//    private RecyclerView rvTrips;
//    private ImageView btnAddTrip;
//    private BottomNavigationView bottomNavigationView;
//    private AppDatabase database;
//    private ExecutorService executorService;
//    private List<Trip> tripList;
//    private TripAdapter adapter;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        rvTrips = findViewById(R.id.rv_trips);
//        btnAddTrip = findViewById(R.id.btn_add_trip);
//        bottomNavigationView = findViewById(R.id.bottom_navigation);
//
//        database = AppDatabase.getInstance(this);
//        executorService = Executors.newSingleThreadExecutor();
//        tripList = new ArrayList<>();
//
//        adapter = new TripAdapter();
//        rvTrips.setLayoutManager(new LinearLayoutManager(this));
//        rvTrips.setAdapter(adapter);
//
//        // Xử lý các sự kiện từ Danh sách (Xóa, Sửa, Click Chi tiết)
//        adapter.setOnTripClickListener(new TripAdapter.OnTripClickListener() {
//            @Override
//            public void onDeleteClick(Trip trip) {
//                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
//                        .setTitle("Xác nhận xóa")
//                        .setMessage("Bạn có chắc muốn xóa chuyến đi này không?")
//                        .setPositiveButton("Có", (dialog, which) -> {
//                            executorService.execute(() -> {
//                                database.tripDao().delete(trip);
//                                loadTrips();
//                            });
//                        })
//                        .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
//                        .show();
//            }
//
//            @Override
//            public void onEditClick(Trip trip) {
//                showTripDialog(trip);
//            }
//
//            @Override
//            public void onTripClick(Trip trip) {
//                Intent intent = new Intent(MainActivity.this, TripDetailActivity.class);
//                intent.putExtra("TRIP_ID", trip.getId());
//                startActivity(intent);
//            }
//        });
//
//        btnAddTrip.setOnClickListener(v -> showTripDialog(null));
//
//        // --- Thêm logic Xử lý sự kiện click cho Bottom Navigation ---
////        if (bottomNavigationView != null) {
////            bottomNavigationView.setSelectedItemId(R.id.nav_home); // Đặt Home là menu mặc định
////            bottomNavigationView.setOnItemSelectedListener(item -> {
////                int itemId = item.getItemId();
////                if (itemId == R.id.nav_home) {
////                    return true;
////                } else if (itemId == R.id.nav_checklist) {
////                    // Chuyển sang màn hình CheckList
////                    Intent intent = new Intent(MainActivity.this, activity_check_list.class);
////                    startActivity(intent);
////                    overridePendingTransition(0, 0); // Bỏ hiệu ứng để cảm giác chuyển mượt như chuyển tab
////                    return true;
////                }
////                // Các menu khác (Expense, Music, More) bạn có thể thêm sau
////                return false;
////            });
////        }
//        if (bottomNavigationView != null) {
//            bottomNavigationView.setSelectedItemId(R.id.nav_home); // Đặt Home là menu mặc định
//            bottomNavigationView.setOnItemSelectedListener(item -> {
//                int itemId = item.getItemId();
//                if (itemId == R.id.nav_home) {
//                    return true;
//                } else if (itemId == R.id.nav_checklist) {
//                    // Chuyển sang màn hình CheckList
//                    Intent intent = new Intent(MainActivity.this, activity_check_list.class);
//                    startActivity(intent);
//                    overridePendingTransition(0, 0);
//                    return true;
//                } else if (itemId == R.id.nav_expense) {
//                    // Chuyển sang màn hình Expense (Quản lý chi tiêu)
//                    Intent intent = new Intent(MainActivity.this, Expense.class);
//                    startActivity(intent);
//                    overridePendingTransition(0, 0);
//                    return true;
//                }
//                return false;
//            });
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        loadTrips();
//    }
//
//    private void loadTrips() {
//        executorService.execute(() -> {
//            List<Trip> dbTrips = database.tripDao().getAllTrips();
//
//            // Đợi nạp dữ liệu mẫu nếu DB trống
//            if (dbTrips.isEmpty()) {
//                try {
//                    Thread.sleep(1000);
//                    dbTrips = database.tripDao().getAllTrips();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            List<Trip> finalTrips = dbTrips;
//            runOnUiThread(() -> {
//                tripList.clear();
//                tripList.addAll(finalTrips);
//                adapter.setTrips(tripList);
//            });
//        });
//    }
//
//    // Hàm hiển thị Dialog dùng chung cho Tạo và Sửa
//    private void showTripDialog(Trip tripToEdit) {
//        Dialog dialog = new Dialog(this);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setContentView(R.layout.dialog_create_trip);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        dialog.getWindow().setLayout(
//                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
//                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
//        );
//
//        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
//        EditText etName = dialog.findViewById(R.id.et_trip_name);
//        EditText etStart = dialog.findViewById(R.id.et_start_date);
//        EditText etEnd = dialog.findViewById(R.id.et_end_date);
//        EditText etBudget = dialog.findViewById(R.id.et_budget);
//        MaterialButton btnAction = dialog.findViewById(R.id.btn_create_trip);
//        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
//        ImageView btnClose = dialog.findViewById(R.id.btn_close);
//
//        // Chế độ chỉnh sửa
//        if (tripToEdit != null) {
//            if (tvTitle != null) tvTitle.setText("Chỉnh Sửa Chuyến Đi");
//            btnAction.setText("Cập nhật");
//            etName.setText(tripToEdit.getName());
//            etStart.setText(tripToEdit.getStartDate());
//            etEnd.setText(tripToEdit.getEndDate());
//            etBudget.setText(tripToEdit.getTotalBudget() == 0 ? "" : String.valueOf((long)tripToEdit.getTotalBudget()));
//        }
//
//        btnClose.setOnClickListener(v -> dialog.dismiss());
//        btnCancel.setOnClickListener(v -> dialog.dismiss());
//
//        etStart.setOnClickListener(v -> showDatePicker(etStart));
//        etEnd.setOnClickListener(v -> showDatePicker(etEnd));
//
//        btnAction.setOnClickListener(v -> {
//            String name = etName.getText().toString().trim();
//            String start = etStart.getText().toString().trim();
//            String end = etEnd.getText().toString().trim();
//            String budgetStr = etBudget.getText().toString().trim();
//
//            // 1. Kiểm tra Tên chuyến đi
//            if (ValidationUtils.isNullOrEmpty(name)) {
//                Toast.makeText(this, "Vui lòng nhập tên chuyến đi!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // 2. Kiểm tra Ngày bắt đầu
//            if (ValidationUtils.isNullOrEmpty(start)) {
//                Toast.makeText(this, "Vui lòng chọn ngày bắt đầu!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Quy tắc: Ngày bắt đầu không được nhỏ hơn ngày hiện tại
//            if (!ValidationUtils.isAfterOrEqualCurrentDate(start, "dd/MM/yyyy")) {
//                Toast.makeText(this, "Ngày bắt đầu không được chọn ngày quá khứ!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // 3. Kiểm tra Ngày kết thúc (Nếu có nhập)
//            if (!ValidationUtils.isNullOrEmpty(end)) {
//                // Quy tắc: End Date >= Start Date
//                if (!ValidationUtils.isStartBeforeOrEqualEnd(start, end, "dd/MM/yyyy")) {
//                    Toast.makeText(this, "Ngày kết thúc phải sau hoặc trùng ngày bắt đầu!", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//            }
//
//            // 4. Kiểm tra Ngân sách và xử lý biến để dùng trong lambda
//            double tempBudget = 0;
//            if (!ValidationUtils.isNullOrEmpty(budgetStr)) {
//                if (ValidationUtils.isNumeric(budgetStr)) {
//                    tempBudget = Double.parseDouble(budgetStr);
//                } else {
//                    Toast.makeText(this, "Ngân sách phải là một con số!", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//            }
//            final double finalBudget = tempBudget; // Biến này sẽ không bị thay đổi nữa (effectively final)
//
//            // Lưu dữ liệu
//            executorService.execute(() -> {
//                if (tripToEdit == null) {
//                    Trip newTrip = new Trip(name, start, end, finalBudget, "Lên kế hoạch");
//                    database.tripDao().insert(newTrip);
//                } else {
//                    tripToEdit.setName(name);
//                    tripToEdit.setStartDate(start);
//                    tripToEdit.setEndDate(end);
//                    tripToEdit.setTotalBudget(finalBudget);
//                    database.tripDao().update(tripToEdit);
//                }
//
//                loadTrips();
//                runOnUiThread(() -> {
//                    dialog.dismiss();
//                    Toast.makeText(this, tripToEdit == null ? "Tạo chuyến đi thành công!" : "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
//                });
//            });
//        });
//
//        dialog.show();
//    }
//
//    private void showDatePicker(EditText editText) {
//        Calendar c = Calendar.getInstance();
//        new DatePickerDialog(this, (view, year, month, day) -> {
//            editText.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
//        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
//    }
//}


package com.tinh.vivu;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tinh.vivu.fragments.ChecklistFragment;
import com.tinh.vivu.fragments.ExpenseFragment;
import com.tinh.vivu.fragments.HomeFragment;
import com.tinh.vivu.fragments.MusicFragment;
import com.tinh.vivu.fragments.MoreFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Áp dụng khoảng đệm (padding) TRỰC TIẾP lên thanh Menu.
        // Tự động đẩy các icon lên cao hơn thanh điều hướng ảo của máy.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // Mặc định nạp Fragment Home khi mở app để phần thân giữa không bị trắng
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();

            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_checklist) {
                selectedFragment = new ChecklistFragment();
            } else if (itemId == R.id.nav_expense) {
                selectedFragment = new ExpenseFragment();
            } else if (itemId == R.id.nav_music) {
                selectedFragment = new MusicFragment();
            } else if (itemId == R.id.nav_more) {
                selectedFragment = new MoreFragment();
            }

           
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}