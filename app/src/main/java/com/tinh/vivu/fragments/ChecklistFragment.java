package com.tinh.vivu.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
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

public class ChecklistFragment extends Fragment implements ChecklistTaskAdapter.OnTaskActionListener {

    private ProgressBar progressBar;
    private TextView tvProgressPercent, tvItemsCompleted;
    private Button btnAddItem, btnAddList;
    private TextView btnResetChecklist; // Khai báo nút reset
    private RecyclerView rvCategories;

    private ChecklistCategoryAdapter categoryAdapter;
    private ChecklistDao checklistDao;
    private ExecutorService executorService;

    private int currentTripId = 1;
    private List<ChecklistCategory> currentCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checklist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupDatabase();
        setupRecyclerView();
        setupListeners();

        loadChecklistData();
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar_checklist);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        tvItemsCompleted = view.findViewById(R.id.tv_items_completed);
        btnAddItem = view.findViewById(R.id.btn_add_item);
        btnAddList = view.findViewById(R.id.btn_add_list);
        btnResetChecklist = view.findViewById(R.id.btn_reset_checklist); // Ánh xạ nút reset
        rvCategories = view.findViewById(R.id.rv_checklist_categories);
    }

    private void setupDatabase() {
        checklistDao = AppDatabase.getInstance(requireContext()).checklistDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    private void setupRecyclerView() {
        categoryAdapter = new ChecklistCategoryAdapter(this, new ChecklistCategoryAdapter.OnCategoryActionListener() {
            @Override
            public void onCategoryReset(ChecklistCategory category) {
                executorService.execute(() -> {
                    checklistDao.resetChecklistByCategory(category.getId());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Đã reset nhóm " + category.getName(), Toast.LENGTH_SHORT).show();
                            loadChecklistData();
                        });
                    }
                });
            }

            @Override
            public void onCategoryEdit(ChecklistCategory category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onCategoryDelete(ChecklistCategory category) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa nhóm '" + category.getName() + "' và toàn bộ đồ đạc bên trong không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            executorService.execute(() -> {
                                checklistDao.deleteTasksByCategoryId(category.getId());
                                checklistDao.deleteCategory(category);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "Đã xóa " + category.getName(), Toast.LENGTH_SHORT).show();
                                        loadChecklistData();
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        btnAddList.setOnClickListener(v -> showAddListDialog());
        btnAddItem.setOnClickListener(v -> showAddItemDialog());


        btnResetChecklist.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận Reset")
                    .setMessage("Bạn có chắc chắn muốn bỏ tích tất cả các món đồ không?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        executorService.execute(() -> {
                            // Gọi hàm reset toàn bộ task của chuyến đi hiện tại
                            checklistDao.resetAllTasksByTripId(currentTripId);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "Đã reset toàn bộ checklist", Toast.LENGTH_SHORT).show();
                                    loadChecklistData();
                                });
                            }
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
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

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    categoryAdapter.setData(categories, taskMap);
                    updateProgressUI(progress, finalCompleted, totalTasks);
                });
            }
        });
    }

    private void updateProgressUI(int percent, int completed, int total) {
        progressBar.setProgress(percent);
        tvProgressPercent.setText(percent + "%");
        tvItemsCompleted.setText(completed + " of " + total + " items completed");
    }

    private void showAddListDialog() {
        Dialog dialog = new Dialog(requireContext());
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
                Toast.makeText(requireContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showEditCategoryDialog(ChecklistCategory category) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_checklist_list);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etListName = dialog.findViewById(R.id.et_list_name);
        Button btnAdd = dialog.findViewById(R.id.btn_add_confirm);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        etListName.setText(category.getName());
        btnAdd.setText("Cập nhật");

        btnAdd.setOnClickListener(v -> {
            String name = etListName.getText().toString().trim();
            if (!name.isEmpty()) {
                category.setName(name);
                executorService.execute(() -> {
                    checklistDao.updateCategory(category);
                    loadChecklistData();
                });
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Vui lòng tạo List (Danh mục) trước", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
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
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryNames);
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
                Toast.makeText(requireContext(), "Vui lòng nhập tên món đồ", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

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
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa món đồ này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    executorService.execute(() -> {
                        checklistDao.deleteTask(task);
                        loadChecklistData();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onTaskEdit(ChecklistTask task) {
        EditText input = new EditText(requireContext());
        input.setText(task.getName());
        input.setPadding(50, 50, 50, 50);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Sửa tên món đồ")
                .setView(input)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        task.setName(newName);
                        executorService.execute(() -> {
                            checklistDao.updateTask(task);
                            loadChecklistData();
                        });
                    } else {
                        Toast.makeText(requireContext(), "Tên không được để trống!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}