package com.tinh.vivu.fragments;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.Expense;
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

public class ExpenseFragment extends Fragment {
    private static final double MAX_AMOUNT = 1_000_000_000_000d;
    private static final int MIN_CATEGORY_NAME_LENGTH = 2;
    private static final int MAX_CATEGORY_NAME_LENGTH = 40;
    private static final int MAX_NOTE_LENGTH = 120;

    private Spinner spinnerFilterTrip;
    private TextView tvTotalSpent, tvSpentFuel, tvSpentFood;
    private TextView tvBudgetAmount, tvBudgetSpent, tvBudgetRemaining;
    private ProgressBar progressBudget;
    private MaterialButton btnAddExpenseMain;
    private Button btnViewSummary;
    private RecyclerView rvExpenses;

    private AppDatabase database;
    private ExecutorService executorService;
    private ExpenseAdapter expenseAdapter;

    private List<Trip> allTrips = new ArrayList<>();
    private List<ExpenseCategory> allCategories = new ArrayList<>();
    private DecimalFormat formatter = new DecimalFormat("#,###");

    private int selectedFilterTripId = -1;

    // Lưu tạm các giá trị tổng để dùng cho Dialog Summary
    private double currentTotalBudget = 0;
    private double currentTotalSpent = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        expenseAdapter = new ExpenseAdapter();
        rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvExpenses.setAdapter(expenseAdapter);

        // Bắt sự kiện click vào khoản chi để Sửa/Xóa
        expenseAdapter.setOnExpenseItemClickListener(expense -> {
            showExpenseOptionsDialog(expense);
        });

        btnAddExpenseMain.setOnClickListener(v -> showAddOrEditExpenseDialog(null));
        btnViewSummary.setOnClickListener(v -> showSummaryDialog());

        loadInitialData();
    }

    private void initViews(View view) {
        spinnerFilterTrip = view.findViewById(R.id.spinner_filter_trip);
        tvTotalSpent = view.findViewById(R.id.tv_total_spent_amount);
        tvSpentFuel = view.findViewById(R.id.tv_spent_fuel);
        tvSpentFood = view.findViewById(R.id.tv_spent_food);
        tvBudgetAmount = view.findViewById(R.id.tv_budget_amount);
        tvBudgetSpent = view.findViewById(R.id.tv_budget_spent);
        tvBudgetRemaining = view.findViewById(R.id.tv_budget_remaining);
        progressBudget = view.findViewById(R.id.progress_budget);
        btnAddExpenseMain = view.findViewById(R.id.btn_add_expense_main);
        btnViewSummary = view.findViewById(R.id.btn_view_summary);
        rvExpenses = view.findViewById(R.id.rv_expenses);
    }

    private void loadInitialData() {
        executorService.execute(() -> {
            allTrips = database.tripDao().getAllTrips();
            allCategories = database.expenseCategoryDao().getAllCategories();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    setupFilterSpinner();
                    loadExpensesData();
                });
            }
        });
    }

    private void setupFilterSpinner() {
        List<String> tripNames = new ArrayList<>();
        tripNames.add(getString(R.string.expense_filter_all_trips));
        for (Trip trip : allTrips) {
            tripNames.add(trip.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, tripNames);
        spinnerFilterTrip.setAdapter(adapter);

        spinnerFilterTrip.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedFilterTripId = -1;
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
            List<Expense> expenses;
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

            final List<Expense> finalExpenses = expenses;
            final double fTotalBudget = totalBudget;
            final double fTotalSpent = totalSpent;
            final double fSpentFuel = spentFuel;
            final double fSpentFood = spentFood;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Cập nhật biến toàn cục
                    currentTotalBudget = fTotalBudget;
                    currentTotalSpent = fTotalSpent;

                    expenseAdapter.setData(finalExpenses, allCategories);
                    updateDashboardUI(fTotalBudget, fTotalSpent, fSpentFuel, fSpentFood);
                });
            }
        });
    }

    private void updateDashboardUI(double budget, double spent, double spentFuel, double spentFood) {
        tvTotalSpent.setText(formatCurrency(spent));
        tvSpentFuel.setText(formatCurrency(spentFuel));
        tvSpentFood.setText(formatCurrency(spentFood));

        tvBudgetAmount.setText(formatCurrency(budget));
        tvBudgetSpent.setText(formatCurrency(spent));

        double remaining = budget - spent;
        if (remaining < 0) {
            tvBudgetRemaining.setText(formatCurrency(remaining));
            tvBudgetRemaining.setTextColor(Color.RED);
        } else {
            tvBudgetRemaining.setText(formatCurrency(remaining));
            tvBudgetRemaining.setTextColor(Color.parseColor("#00AA55"));
        }

        // --- CẬP NHẬT MÀU SẮC CHO THANH TIẾN TRÌNH ---
        // PorterDuff.Mode.SRC đảm bảo màu được phủ đầy (solid) và không bị nhạt do hiệu ứng nền mặc định của Android
        progressBudget.setProgressBackgroundTintMode(PorterDuff.Mode.SRC);

        if (budget > 0) {
            int progress = (int) ((spent / budget) * 100);
            progressBudget.setProgress(Math.min(progress, 100));

            if (spent == 0) {
                // 1. Chưa chi đồng nào: Xanh dương nguyên cây (chỉnh màu Background)
                progressBudget.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0055FF")));
                // Set hờ cho tiến độ mặc dù nó đang = 0
                progressBudget.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#0055FF")));
            } else {
                // 2. Đã chi:
                // - Phần đã chi (Progress) = Đỏ
                // - Phần còn lại (Background) = Xanh lá
                progressBudget.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF4444")));
                progressBudget.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00AA55")));
            }
        } else {
            progressBudget.setProgress(0);
            progressBudget.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0"))); // Xám
            progressBudget.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        }
    }

    // Hiển thị Menu Sửa/Xóa khi click vào 1 khoản chi
    private void showExpenseOptionsDialog(Expense expense) {
        String[] options = {
                getString(R.string.expense_option_edit),
                getString(R.string.expense_option_delete)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.expense_options_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddOrEditExpenseDialog(expense); // Gọi hàm sửa
                    } else {
                        deleteExpense(expense); // Gọi hàm xóa
                    }
                })
                .show();
    }

    private void deleteExpense(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.expense_delete_confirm_title)
                .setMessage(getString(R.string.expense_delete_confirm_message_format, expense.getNote()))
                .setPositiveButton(R.string.common_delete, (dialog, which) -> {
                    executorService.execute(() -> {
                        database.expenseDao().delete(expense);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), getString(R.string.expense_toast_deleted), Toast.LENGTH_SHORT).show();
                                loadExpensesData();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    // Nâng cấp hàm thành Thêm hoặc Sửa (nếu expenseToEdit != null thì là Sửa)
    private void showAddOrEditExpenseDialog(@Nullable Expense expenseToEdit) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_expense);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        Spinner spinnerTrip = dialog.findViewById(R.id.spinner_trip);
        Spinner spinnerCategory = dialog.findViewById(R.id.spinner_category);
        EditText etAmount = dialog.findViewById(R.id.et_amount);
        EditText etNote = dialog.findViewById(R.id.et_note);
        MaterialButton btnAdd = dialog.findViewById(R.id.btn_add);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        TextView tvManageCategory = dialog.findViewById(R.id.tv_manage_category);

        // Thiết lập danh sách Chuyến đi cho Spinner
        List<String> tripNames = new ArrayList<>();
        tripNames.add(getString(R.string.expense_trip_optional));
        for (int i = 0; i < allTrips.size(); i++) {
            tripNames.add(allTrips.get(i).getName());
        }
        spinnerTrip.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, tripNames));

        // Thiết lập danh sách Danh mục cho Spinner
        ArrayAdapter<ExpenseCategory> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, allCategories);
        spinnerCategory.setAdapter(catAdapter);

        // --- NẾU LÀ SỬA: ĐIỀN SẴN DỮ LIỆU CŨ ---
        if (expenseToEdit != null) {
            btnAdd.setText(getString(R.string.common_update));
            etAmount.setText(String.valueOf((long) expenseToEdit.getAmount()));
            etNote.setText(expenseToEdit.getNote());

            // Chọn đúng Chuyến đi
            if (expenseToEdit.getTripId() != null) {
                for (int i = 0; i < allTrips.size(); i++) {
                    if (allTrips.get(i).getId() == expenseToEdit.getTripId()) {
                        spinnerTrip.setSelection(i + 1); // +1 vì index 0 là "Chọn chuyến đi"
                        break;
                    }
                }
            }

            // Chọn đúng Danh mục
            for (int i = 0; i < allCategories.size(); i++) {
                if (allCategories.get(i).getCategoryId() == expenseToEdit.getCategoryId()) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } else {
            // NẾU LÀ THÊM MỚI: Tự động chọn chuyến đi đang được lọc (nếu có)
            if (selectedFilterTripId != -1) {
                for (int i = 0; i < allTrips.size(); i++) {
                    if (allTrips.get(i).getId() == selectedFilterTripId) {
                        spinnerTrip.setSelection(i + 1);
                        break;
                    }
                }
            }
        }

        if (tvManageCategory != null) {
            tvManageCategory.setOnClickListener(v -> showManageCategoryDialog(catAdapter));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            clearErrors(etAmount, etNote);

            if (ValidationUtils.isNullOrEmpty(amountStr)) {
                showFieldError(etAmount, getString(R.string.expense_error_amount_required));
                return;
            }

            if (!ValidationUtils.isValidDecimalFormat(amountStr, 12, 2) || !ValidationUtils.isNumeric(amountStr)) {
                showFieldError(etAmount, getString(R.string.expense_error_amount_invalid_format));
                return;
            }

            double amount = Double.parseDouble(amountStr);

            if (!ValidationUtils.isPositiveNumber(amount)) {
                showFieldError(etAmount, getString(R.string.expense_error_amount_positive));
                return;
            }
            if (!ValidationUtils.isNumberInRange(amount, 0.01d, MAX_AMOUNT)) {
                showFieldError(etAmount, getString(R.string.expense_error_amount_out_of_range));
                return;
            }
            if (!ValidationUtils.isNullOrEmpty(note)
                    && !ValidationUtils.isValidDisplayName(note, 2, MAX_NOTE_LENGTH)) {
                showFieldError(etNote, getString(R.string.expense_error_note_invalid));
                return;
            }

            ExpenseCategory selectedCat = (ExpenseCategory) spinnerCategory.getSelectedItem();

            Integer tripId = null;
            int tripPos = spinnerTrip.getSelectedItemPosition();
            if (tripPos > 0) {
                tripId = allTrips.get(tripPos - 1).getId();
            }

            // Tạo các biến final để dùng trong lambda
            final double finalAmount = amount;
            final String finalNote = note;
            final Integer finalTripId = tripId;
            final int finalCategoryId = selectedCat.getCategoryId();

            executorService.execute(() -> {
                if (expenseToEdit != null) {
                    // Update
                    expenseToEdit.setAmount(finalAmount);
                    expenseToEdit.setNote(finalNote);
                    expenseToEdit.setTripId(finalTripId);
                    expenseToEdit.setCategoryId(finalCategoryId);
                    database.expenseDao().update(expenseToEdit);
                } else {
                    // Insert
                    String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    Expense newExpense = new Expense(finalTripId, finalCategoryId, finalAmount, currentDate, finalNote);
                    database.expenseDao().insert(newExpense);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), expenseToEdit != null
                                ? getString(R.string.expense_toast_updated)
                                : getString(R.string.expense_toast_added), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadExpensesData();
                    });
                }
            });
        });

        dialog.show();
    }

    // Hiển thị Dialog Tổng kết với Thanh tiến trình (Sử dụng layout mới)
    private void showSummaryDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_summary);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        TextView tvTitle = dialog.findViewById(R.id.tv_summary_title);
        TextView tvBudget = dialog.findViewById(R.id.tv_summary_budget);
        TextView tvSpent = dialog.findViewById(R.id.tv_summary_spent);
        TextView tvRemaining = dialog.findViewById(R.id.tv_summary_remaining);
        ProgressBar progressBar = dialog.findViewById(R.id.progress_summary_budget);
        MaterialButton btnClose = dialog.findViewById(R.id.btn_summary_close);

        String title = selectedFilterTripId == -1
                ? getString(R.string.expense_summary_all_trips_title)
                : getString(R.string.expense_summary_trip_title);
        tvTitle.setText(title);

        tvBudget.setText(formatCurrency(currentTotalBudget));
        tvSpent.setText(formatCurrency(currentTotalSpent));

        double remaining = currentTotalBudget - currentTotalSpent;
        if (remaining < 0) {
            tvRemaining.setText(formatCurrency(remaining));
            tvRemaining.setTextColor(Color.RED);
        } else {
            tvRemaining.setText(formatCurrency(remaining));
            tvRemaining.setTextColor(Color.parseColor("#00AA55"));
        }

        if (currentTotalBudget > 0) {
            int progress = (int) ((currentTotalSpent / currentTotalBudget) * 100);
            progressBar.setProgress(Math.min(progress, 100));
        } else {
            progressBar.setProgress(0);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // CÁC HÀM QUẢN LÝ DANH MỤC GIỮ NGUYÊN BÊN DƯỚI
    private void showManageCategoryDialog(ArrayAdapter<ExpenseCategory> catAdapter) {
        String[] catNames = new String[allCategories.size()];
        for (int i = 0; i < allCategories.size(); i++) {
            catNames[i] = allCategories.get(i).getCategoryName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.expense_category_manage_title)
                .setItems(catNames, (dialog, which) -> {
                    showEditDeleteCategoryDialog(allCategories.get(which), catAdapter);
                })
                .setPositiveButton(R.string.expense_category_add_new_button, (dialog, which) -> {
                    showAddCategoryDialog(catAdapter);
                })
                .setNegativeButton(R.string.common_close, null)
                .show();
    }

    private void showAddCategoryDialog(ArrayAdapter<ExpenseCategory> catAdapter) {
        EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.expense_category_new_hint));
        input.setPadding(50, 50, 50, 50);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.expense_category_add_title)
                .setView(input)
                .setPositiveButton(R.string.common_save, null)
                .setNegativeButton(R.string.common_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            List<String> existingNames = getCategoryNamesExcept(null);

            if (ValidationUtils.isNullOrEmpty(newName)) {
                input.setError(getString(R.string.expense_error_category_required));
                return;
            }
            if (!ValidationUtils.isValidDisplayName(newName, MIN_CATEGORY_NAME_LENGTH, MAX_CATEGORY_NAME_LENGTH)) {
                input.setError(getString(R.string.expense_error_category_invalid));
                return;
            }
            if (ValidationUtils.isDuplicateName(newName, existingNames)) {
                input.setError(getString(R.string.expense_error_category_exists));
                return;
            }

            executorService.execute(() -> {
                ExpenseCategory newCat = new ExpenseCategory(newName);
                database.expenseCategoryDao().insert(newCat);
                reloadCategoriesAndUpdateUI(catAdapter);
            });
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showEditDeleteCategoryDialog(ExpenseCategory category, ArrayAdapter<ExpenseCategory> catAdapter) {
        EditText input = new EditText(requireContext());
        input.setText(category.getCategoryName());
        input.setPadding(50, 50, 50, 50);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.expense_category_edit_delete_title)
                .setView(input)
                .setPositiveButton(R.string.common_update, null)
                .setNeutralButton(R.string.common_delete, (dialogInterface, which) -> {
                    executorService.execute(() -> {
                        try {
                            database.expenseCategoryDao().delete(category);
                            reloadCategoriesAndUpdateUI(catAdapter);
                        } catch (Exception e) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.expense_error_category_delete_in_use), Toast.LENGTH_LONG).show());
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.common_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String updatedName = input.getText().toString().trim();
            List<String> existingNames = getCategoryNamesExcept(category);

            if (ValidationUtils.isNullOrEmpty(updatedName)) {
                input.setError(getString(R.string.expense_error_category_required));
                return;
            }
            if (!ValidationUtils.isValidDisplayName(updatedName, MIN_CATEGORY_NAME_LENGTH, MAX_CATEGORY_NAME_LENGTH)) {
                input.setError(getString(R.string.expense_error_category_invalid));
                return;
            }
            if (ValidationUtils.isDuplicateName(updatedName, existingNames)) {
                input.setError(getString(R.string.expense_error_category_exists));
                return;
            }

            category.setCategoryName(updatedName);
            executorService.execute(() -> {
                database.expenseCategoryDao().update(category);
                reloadCategoriesAndUpdateUI(catAdapter);
            });
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void reloadCategoriesAndUpdateUI(ArrayAdapter<ExpenseCategory> catAdapter) {
        List<ExpenseCategory> updatedCategories = database.expenseCategoryDao().getAllCategories();
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                allCategories.clear();
                allCategories.addAll(updatedCategories);
                catAdapter.notifyDataSetChanged();
                Toast.makeText(requireContext(), getString(R.string.expense_toast_category_updated), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String formatCurrency(double value) {
        return getString(R.string.expense_currency_amount_format, formatter.format(value));
    }

    private List<String> getCategoryNamesExcept(@Nullable ExpenseCategory excludedCategory) {
        List<String> names = new ArrayList<>();
        for (ExpenseCategory category : allCategories) {
            if (excludedCategory != null && category.getCategoryId() == excludedCategory.getCategoryId()) {
                continue;
            }
            names.add(category.getCategoryName());
        }
        return names;
    }

    private void clearErrors(EditText... fields) {
        for (EditText field : fields) {
            if (field != null) {
                field.setError(null);
            }
        }
    }

    private void showFieldError(EditText field, String message) {
        if (field != null) {
            field.setError(message);
            field.requestFocus();
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}