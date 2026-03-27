package com.tinh.vivu;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.ExpenseCategory;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.utils.ValidationUtils;
import com.tinh.vivu.views.ExpenseAdapter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Đã đổi tên class thành Expense theo đúng file của bạn tạo
public class Expense extends AppCompatActivity {

    private Spinner spinnerFilterTrip;
    private TextView tvTotalSpent, tvSpentFuel, tvSpentFood;
    private TextView tvBudgetAmount, tvBudgetSpent, tvBudgetRemaining;
    private ProgressBar progressBudget;
    private MaterialButton btnAddExpenseMain;
    private Button btnViewSummary;
    private RecyclerView rvExpenses;
    private BottomNavigationView bottomNavigationView;

    private AppDatabase database;
    private ExecutorService executorService;
    private ExpenseAdapter expenseAdapter;

    private List<Trip> allTrips = new ArrayList<>();
    private List<ExpenseCategory> allCategories = new ArrayList<>();
    private DecimalFormat formatter = new DecimalFormat("#,###");

    // -1 nghĩa là đang chọn "Tất cả chuyến đi"
    private int selectedFilterTripId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_expense);

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        initViews();
        setupBottomNavigation();

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        expenseAdapter = new ExpenseAdapter();
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);

        // Bắt sự kiện cho các nút
        btnAddExpenseMain.setOnClickListener(v -> showAddExpenseDialog());
        btnViewSummary.setOnClickListener(v -> showSummaryDialog());

        // Load dữ liệu ban đầu
        loadInitialData();
    }

    private void initViews() {
        spinnerFilterTrip = findViewById(R.id.spinner_filter_trip);
        tvTotalSpent = findViewById(R.id.tv_total_spent_amount);
        tvSpentFuel = findViewById(R.id.tv_spent_fuel);
        tvSpentFood = findViewById(R.id.tv_spent_food);
        tvBudgetAmount = findViewById(R.id.tv_budget_amount);
        tvBudgetSpent = findViewById(R.id.tv_budget_spent);
        tvBudgetRemaining = findViewById(R.id.tv_budget_remaining);
        progressBudget = findViewById(R.id.progress_budget);
        btnAddExpenseMain = findViewById(R.id.btn_add_expense_main);
        btnViewSummary = findViewById(R.id.btn_view_summary);
        rvExpenses = findViewById(R.id.rv_expenses);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_expense);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_expense) {
                return true;
            } else if (itemId == R.id.nav_home) {
                Intent intent = new Intent(Expense.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_checklist) {
                Intent intent = new Intent(Expense.this, activity_check_list.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadInitialData() {
        executorService.execute(() -> {
            allTrips = database.tripDao().getAllTrips();
            allCategories = database.expenseCategoryDao().getAllCategories();

            runOnUiThread(() -> {
                setupFilterSpinner();
                loadExpensesData();
            });
        });
    }

    private void setupFilterSpinner() {
        List<String> tripNames = new ArrayList<>();
        tripNames.add("Tất cả chuyến đi");
        for (Trip trip : allTrips) {
            tripNames.add(trip.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tripNames);
        spinnerFilterTrip.setAdapter(adapter);

        spinnerFilterTrip.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedFilterTripId = -1; // Tất cả
                } else {
                    selectedFilterTripId = allTrips.get(position - 1).getId();
                }
                loadExpensesData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadExpensesData() {
        executorService.execute(() -> {
            List<com.tinh.vivu.models.Expense> expenses;
            double totalBudget = 0;
            double totalSpent = 0;
            double spentFuel = 0;
            double spentFood = 0;

            int fuelCatId = -1, foodCatId = -1;
            for (ExpenseCategory cat : allCategories) {
                if (cat.getCategoryName().toLowerCase().contains("xăng")) fuelCatId = cat.getCategoryId();
                if (cat.getCategoryName().toLowerCase().contains("ăn")) foodCatId = cat.getCategoryId();
            }

            if (selectedFilterTripId == -1) {
                expenses = database.expenseDao().getAllExpenses();
                for (Trip trip : allTrips) totalBudget += trip.getTotalBudget();
                totalSpent = database.expenseDao().getTotalSpentAll();

                if (fuelCatId != -1) spentFuel = database.expenseDao().getTotalSpentByCategoryAll(fuelCatId);
                if (foodCatId != -1) spentFood = database.expenseDao().getTotalSpentByCategoryAll(foodCatId);
            } else {
                expenses = database.expenseDao().getExpensesByTripId(selectedFilterTripId);
                for (Trip trip : allTrips) {
                    if (trip.getId() == selectedFilterTripId) {
                        totalBudget = trip.getTotalBudget();
                        break;
                    }
                }
                totalSpent = database.expenseDao().getTotalSpentByTripId(selectedFilterTripId);

                if (fuelCatId != -1) spentFuel = database.expenseDao().getTotalSpentByCategoryAndTrip(fuelCatId, selectedFilterTripId);
                if (foodCatId != -1) spentFood = database.expenseDao().getTotalSpentByCategoryAndTrip(foodCatId, selectedFilterTripId);
            }

            final List<com.tinh.vivu.models.Expense> finalExpenses = expenses;
            final double fTotalBudget = totalBudget;
            final double fTotalSpent = totalSpent;
            final double fSpentFuel = spentFuel;
            final double fSpentFood = spentFood;

            runOnUiThread(() -> {
                expenseAdapter.setData(finalExpenses, allCategories);
                updateDashboardUI(fTotalBudget, fTotalSpent, fSpentFuel, fSpentFood);
            });
        });
    }

    private void updateDashboardUI(double budget, double spent, double spentFuel, double spentFood) {
        tvTotalSpent.setText(formatter.format(spent) + " đ");
        tvSpentFuel.setText(formatter.format(spentFuel) + " đ");
        tvSpentFood.setText(formatter.format(spentFood) + " đ");

        tvBudgetAmount.setText(formatter.format(budget) + " đ");
        tvBudgetSpent.setText(formatter.format(spent) + " đ");

        double remaining = budget - spent;
        if (remaining < 0) {
            tvBudgetRemaining.setText(formatter.format(remaining) + " đ");
            tvBudgetRemaining.setTextColor(Color.RED);
        } else {
            tvBudgetRemaining.setText(formatter.format(remaining) + " đ");
            tvBudgetRemaining.setTextColor(Color.parseColor("#00AA55"));
        }

        if (budget > 0) {
            int progress = (int) ((spent / budget) * 100);
            progressBudget.setProgress(Math.min(progress, 100));
        } else {
            progressBudget.setProgress(0);
        }
    }

    // Hiển thị Popup Xem Tổng Kết (Tạm dùng AlertDialog cơ bản)
    private void showSummaryDialog() {
        String title = selectedFilterTripId == -1 ? "Tổng Kết Tất Cả Chuyến Đi" : "Tổng Kết Chuyến Đi";
        String message = "Tổng Ngân sách dự kiến: " + tvBudgetAmount.getText().toString() + "\n\n"
                + "Tổng Đã chi: " + tvBudgetSpent.getText().toString() + "\n\n"
                + "Số tiền còn lại: " + tvBudgetRemaining.getText().toString();

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showAddExpenseDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_expense);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        Spinner spinnerTrip = dialog.findViewById(R.id.spinner_trip);
        Spinner spinnerCategory = dialog.findViewById(R.id.spinner_category);
        EditText etAmount = dialog.findViewById(R.id.et_amount);
        EditText etNote = dialog.findViewById(R.id.et_note);
        MaterialButton btnAdd = dialog.findViewById(R.id.btn_add);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        TextView tvManageCategory = dialog.findViewById(R.id.tv_manage_category); // Ánh xạ nút Quản lý

        // Setup Spinner Trip trong Dialog
        List<String> tripNames = new ArrayList<>();
        tripNames.add("Chọn chuyến đi (không bắt buộc)");
        int tripSelectionIndex = 0;
        for (int i = 0; i < allTrips.size(); i++) {
            tripNames.add(allTrips.get(i).getName());
            if (allTrips.get(i).getId() == selectedFilterTripId) {
                tripSelectionIndex = i + 1;
            }
        }
        spinnerTrip.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tripNames));
        spinnerTrip.setSelection(tripSelectionIndex);

        // Setup Spinner Category
        ArrayAdapter<ExpenseCategory> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, allCategories);
        spinnerCategory.setAdapter(catAdapter);

        // Bắt sự kiện Quản lý danh mục
        if (tvManageCategory != null) {
            tvManageCategory.setOnClickListener(v -> showManageCategoryDialog(catAdapter));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            // SỬ DỤNG VALIDATION UTILS
            if (ValidationUtils.isNullOrEmpty(amountStr)) {
                Toast.makeText(this, "Vui lòng nhập số tiền!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isNumeric(amountStr)) {
                Toast.makeText(this, "Số tiền không đúng định dạng!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            if (!ValidationUtils.isPositiveNumber(amount)) {
                Toast.makeText(this, "Số tiền phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
                return;
            }

            ExpenseCategory selectedCat = (ExpenseCategory) spinnerCategory.getSelectedItem();

            Integer tripId = null;
            int tripPos = spinnerTrip.getSelectedItemPosition();
            if (tripPos > 0) {
                tripId = allTrips.get(tripPos - 1).getId();
            }

            String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            com.tinh.vivu.models.Expense newExpense = new com.tinh.vivu.models.Expense(tripId, selectedCat.getCategoryId(), amount, currentDate, note);

            executorService.execute(() -> {
                database.expenseDao().insert(newExpense);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã thêm khoản chi", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadExpensesData(); // Tải lại dữ liệu
                });
            });
        });

        dialog.show();
    }

    // --- CÁC HÀM QUẢN LÝ DANH MỤC (THÊM/SỬA/XÓA) ---

    private void showManageCategoryDialog(ArrayAdapter<ExpenseCategory> catAdapter) {
        String[] catNames = new String[allCategories.size()];
        for (int i = 0; i < allCategories.size(); i++) {
            catNames[i] = allCategories.get(i).getCategoryName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Quản lý loại chi tiêu")
                .setItems(catNames, (dialog, which) -> {
                    // Bấm vào 1 item sẽ mở Form Sửa / Xóa
                    showEditDeleteCategoryDialog(allCategories.get(which), catAdapter);
                })
                .setPositiveButton("+ Thêm mới", (dialog, which) -> {
                    showAddCategoryDialog(catAdapter);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showAddCategoryDialog(ArrayAdapter<ExpenseCategory> catAdapter) {
        EditText input = new EditText(this);
        input.setHint("Nhập tên loại chi tiêu mới...");
        input.setPadding(50, 50, 50, 50); // Cho ô nhập liệu rộng ra xíu

        new AlertDialog.Builder(this)
                .setTitle("Thêm Loại Chi Tiêu")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        executorService.execute(() -> {
                            ExpenseCategory newCat = new ExpenseCategory(newName);
                            database.expenseCategoryDao().insert(newCat);
                            reloadCategoriesAndUpdateUI(catAdapter);
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditDeleteCategoryDialog(ExpenseCategory category, ArrayAdapter<ExpenseCategory> catAdapter) {
        EditText input = new EditText(this);
        input.setText(category.getCategoryName());
        input.setPadding(50, 50, 50, 50);

        new AlertDialog.Builder(this)
                .setTitle("Sửa / Xóa Danh Mục")
                .setView(input)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String updatedName = input.getText().toString().trim();
                    if (!updatedName.isEmpty()) {
                        category.setCategoryName(updatedName);
                        executorService.execute(() -> {
                            database.expenseCategoryDao().update(category);
                            reloadCategoriesAndUpdateUI(catAdapter);
                        });
                    }
                })
                .setNeutralButton("Xóa", (dialog, which) -> {
                    executorService.execute(() -> {
                        try {
                            database.expenseCategoryDao().delete(category);
                            reloadCategoriesAndUpdateUI(catAdapter);
                        } catch (Exception e) {
                            // Lỗi xảy ra khi xóa danh mục mà danh mục đó đã có khoản chi
                            runOnUiThread(() -> Toast.makeText(Expense.this, "Không thể xóa do danh mục này đang được dùng ở các khoản chi!", Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void reloadCategoriesAndUpdateUI(ArrayAdapter<ExpenseCategory> catAdapter) {
        List<ExpenseCategory> updatedCategories = database.expenseCategoryDao().getAllCategories();
        runOnUiThread(() -> {
            allCategories.clear();
            allCategories.addAll(updatedCategories);
            catAdapter.notifyDataSetChanged(); // Cập nhật lại list ở Dropdown ngay lập tức
            Toast.makeText(Expense.this, "Đã cập nhật danh mục!", Toast.LENGTH_SHORT).show();
        });
    }
}