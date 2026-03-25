package com.tinh.vivu;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;

import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.data.ChecklistDao;
import com.tinh.vivu.models.ChecklistCategory;
import com.tinh.vivu.models.ChecklistTask;
import com.tinh.vivu.views.ChecklistCategoryAdapter;
import com.tinh.vivu.views.ChecklistTaskAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class activity_check_list extends AppCompatActivity implements ChecklistTaskAdapter.OnTaskActionListener {

    private ProgressBar progressBar;
    private TextView tvProgressPercent, tvItemsCompleted;
    private Button btnAddItem, btnAddList;
    private RecyclerView rvCategories;
    private BottomNavigationView bottomNavigationView;

    private ChecklistCategoryAdapter categoryAdapter;
    private ChecklistDao checklistDao;
    private ExecutorService executorService;

    private int currentTripId = 1;
    private List<ChecklistCategory> currentCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_list);

        initViews();
        setupDatabase();
        setupRecyclerView();
        setupListeners();

        loadChecklistData();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progress_bar_checklist);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        tvItemsCompleted = findViewById(R.id.tv_items_completed);
        btnAddItem = findViewById(R.id.btn_add_item);
        btnAddList = findViewById(R.id.btn_add_list);
        rvCategories = findViewById(R.id.rv_checklist_categories);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupDatabase() {
        checklistDao = AppDatabase.getInstance(this).checklistDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    private void setupRecyclerView() {
        // Truyền cả 2 Listener (Xử lý Item và Xử lý Category Menu)
        categoryAdapter = new ChecklistCategoryAdapter(this, new ChecklistCategoryAdapter.OnCategoryActionListener() {
            @Override
            public void onCategoryReset(ChecklistCategory category) {
                executorService.execute(() -> {
                    checklistDao.resetChecklistByCategory(category.getId());
                    runOnUiThread(() -> {
                        Toast.makeText(activity_check_list.this, "Đã reset nhóm " + category.getName(), Toast.LENGTH_SHORT).show();
                        loadChecklistData();
                    });
                });
            }

            @Override
            public void onCategoryEdit(ChecklistCategory category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onCategoryDelete(ChecklistCategory category) {
                // Hiển thị Popup xác nhận trước khi xóa
                new androidx.appcompat.app.AlertDialog.Builder(activity_check_list.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa nhóm '" + category.getName() + "' và toàn bộ đồ đạc bên trong nhóm này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            executorService.execute(() -> {
                                // Xóa các mục con trước, sau đó xóa mục cha
                                checklistDao.deleteTasksByCategoryId(category.getId());
                                checklistDao.deleteCategory(category);
                                runOnUiThread(() -> {
                                    Toast.makeText(activity_check_list.this, "Đã xóa " + category.getName(), Toast.LENGTH_SHORT).show();
                                    loadChecklistData();
                                });
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        btnAddList.setOnClickListener(v -> showAddListDialog());
        btnAddItem.setOnClickListener(v -> showAddItemDialog());

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_checklist);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_checklist) {
                    return true;
                } else if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(activity_check_list.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadChecklistData() {
        executorService.execute(() -> {
            List<ChecklistCategory> categories = checklistDao.getCategoriesByTripId(currentTripId);
            List<ChecklistTask> allTasks = checklistDao.getAllTasksByTripId(currentTripId);

            currentCategories = categories;
            Map<Integer, List<ChecklistTask>> taskMap = new HashMap<>();

            for (ChecklistCategory category : categories) {
                taskMap.put(category.getId(), new ArrayList<>());
            }

            int totalTasks = allTasks.size();
            int completedTasks = 0;

            for (ChecklistTask task : allTasks) {
                if (taskMap.containsKey(task.getCategoryId())) {
                    taskMap.get(task.getCategoryId()).add(task);
                }
                if (task.isCompleted()) {
                    completedTasks++;
                }
            }

            int finalCompleted = completedTasks;
            int progress = totalTasks > 0 ? (int) (((float) finalCompleted / totalTasks) * 100) : 0;

            runOnUiThread(() -> {
                categoryAdapter.setData(categories, taskMap);
                updateProgressUI(progress, finalCompleted, totalTasks);
            });
        });
    }

    private void updateProgressUI(int percent, int completed, int total) {
        progressBar.setProgress(percent);
        tvProgressPercent.setText(percent + "%");
        tvItemsCompleted.setText(completed + " of " + total + " items completed");
    }

    // Dialog DÙNG CHUNG cho Thêm List Mới
    private void showAddListDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_checklist_list);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etListName = dialog.findViewById(R.id.et_list_name);
        Button btnAdd = dialog.findViewById(R.id.btn_add_confirm);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        btnAdd.setOnClickListener(v -> {
            String name = etListName.getText().toString().trim();
            if (!name.isEmpty()) {
                executorService.execute(() -> {
                    ChecklistCategory newCategory = new ChecklistCategory(currentTripId, name);
                    checklistDao.insertCategory(newCategory);
                    loadChecklistData();
                });
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Dialog tái sử dụng cho tính năng Đổi Tên (Sửa)
    private void showEditCategoryDialog(ChecklistCategory category) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_checklist_list);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etListName = dialog.findViewById(R.id.et_list_name);
        Button btnAdd = dialog.findViewById(R.id.btn_add_confirm);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        // Đổ tên cũ vào ô nhập & đổi chữ nút bấm
        etListName.setText(category.getName());
        btnAdd.setText("Cập nhật");

        btnAdd.setOnClickListener(v -> {
            String name = etListName.getText().toString().trim();
            if (!name.isEmpty()) {
                category.setName(name); // Gán tên mới
                executorService.execute(() -> {
                    checklistDao.updateCategory(category);
                    loadChecklistData();
                });
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showAddItemDialog() {
        if (currentCategories == null || currentCategories.isEmpty()) {
            Toast.makeText(this, "Vui lòng tạo List (Danh mục) trước", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_checklist_item);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etItemName = dialog.findViewById(R.id.et_item_name);
        Spinner spCategories = dialog.findViewById(R.id.sp_categories);
        Button btnAdd = dialog.findViewById(R.id.btn_add_confirm);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        List<String> categoryNames = new ArrayList<>();
        for (ChecklistCategory cat : currentCategories) {
            categoryNames.add(cat.getName());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spCategories.setAdapter(spinnerAdapter);

        btnAdd.setOnClickListener(v -> {
            String itemName = etItemName.getText().toString().trim();
            int selectedPosition = spCategories.getSelectedItemPosition();

            if (!itemName.isEmpty() && selectedPosition >= 0) {
                int categoryId = currentCategories.get(selectedPosition).getId();
                executorService.execute(() -> {
                    ChecklistTask newTask = new ChecklistTask(categoryId, currentTripId, itemName);
                    checklistDao.insertTask(newTask);
                    loadChecklistData();
                });
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng nhập tên món đồ", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Các hàm của OnTaskActionListener
    @Override
    public void onTaskStatusChanged(ChecklistTask task, boolean isChecked) {
        task.setCompleted(isChecked);
        task.setCompletedAt(isChecked ? System.currentTimeMillis() : 0);
        executorService.execute(() -> {
            checklistDao.updateTask(task);
            loadChecklistData();
        });
    }

    @Override
    public void onTaskDeleted(ChecklistTask task) {
        executorService.execute(() -> {
            checklistDao.deleteTask(task);
            loadChecklistData();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}