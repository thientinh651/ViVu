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
import com.tinh.vivu.utils.ValidationUtils;
import com.tinh.vivu.views.ChecklistCategoryAdapter;
import com.tinh.vivu.views.ChecklistTaskAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChecklistFragment extends Fragment implements ChecklistTaskAdapter.OnTaskActionListener {
    private static final int MIN_CATEGORY_NAME_LENGTH = 2;
    private static final int MAX_CATEGORY_NAME_LENGTH = 40;
    private static final int MIN_ITEM_NAME_LENGTH = 2;
    private static final int MAX_ITEM_NAME_LENGTH = 60;

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
    private Map<Integer, List<ChecklistTask>> currentTaskMap = new HashMap<>();

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
                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.checklist_toast_reset_group, category.getName()),
                                        Toast.LENGTH_SHORT
                                ).show();
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
                        .setTitle(R.string.checklist_dialog_delete_title)
                        .setMessage(getString(R.string.checklist_dialog_delete_category_message, category.getName()))
                        .setPositiveButton(R.string.common_delete, (dialog, which) -> {
                            executorService.execute(() -> {
                                checklistDao.deleteTasksByCategoryId(category.getId());
                                checklistDao.deleteCategory(category);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(
                                                requireContext(),
                                                getString(R.string.checklist_toast_deleted_group, category.getName()),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        loadChecklistData();
                                    });
                                }
                            });
                        })
                        .setNegativeButton(R.string.common_cancel, null)
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
                    .setTitle(R.string.checklist_dialog_reset_title)
                    .setMessage(R.string.checklist_dialog_reset_message)
                    .setPositiveButton(R.string.common_reset, (dialog, which) -> {
                        executorService.execute(() -> {
                            // Gọi hàm reset toàn bộ task của chuyến đi hiện tại
                            checklistDao.resetTasksByTripId(currentTripId);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), R.string.checklist_toast_reset_all, Toast.LENGTH_SHORT).show();
                                    loadChecklistData();
                                });
                            }
                        });
                    })
                    .setNegativeButton(R.string.common_cancel, null)
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
                    currentTaskMap = taskMap;
                    categoryAdapter.setData(categories, taskMap);
                    updateProgressUI(progress, finalCompleted, totalTasks);
                });
            }
        });
    }

    private void updateProgressUI(int percent, int completed, int total) {
        progressBar.setProgress(percent);
        tvProgressPercent.setText(percent + "%");
        tvItemsCompleted.setText(getString(R.string.checklist_items_completed_format, completed, total));
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
            etListName.setError(null);
            if (ValidationUtils.isNullOrEmpty(name)) {
                etListName.setError(getString(R.string.checklist_error_enter_category_name));
            } else if (!ValidationUtils.isValidDisplayName(name, MIN_CATEGORY_NAME_LENGTH, MAX_CATEGORY_NAME_LENGTH)) {
                etListName.setError(getString(R.string.checklist_error_category_name_invalid));
            } else if (ValidationUtils.isDuplicateName(name, getCategoryNamesExcept(null))) {
                etListName.setError(getString(R.string.checklist_error_category_exists));
            } else {
                executorService.execute(() -> {
                    ChecklistCategory newCategory = new ChecklistCategory(currentTripId, name);
                    checklistDao.insertCategory(newCategory);
                    loadChecklistData();
                });
                dialog.dismiss();
                return;
            }
            Toast.makeText(requireContext(), etListName.getError(), Toast.LENGTH_SHORT).show();
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
        btnAdd.setText(R.string.common_update);

        btnAdd.setOnClickListener(v -> {
            String name = etListName.getText().toString().trim();
            etListName.setError(null);
            if (ValidationUtils.isNullOrEmpty(name)) {
                etListName.setError(getString(R.string.checklist_error_enter_category_name));
            } else if (!ValidationUtils.isValidDisplayName(name, MIN_CATEGORY_NAME_LENGTH, MAX_CATEGORY_NAME_LENGTH)) {
                etListName.setError(getString(R.string.checklist_error_category_name_invalid));
            } else if (ValidationUtils.isDuplicateName(name, getCategoryNamesExcept(category))) {
                etListName.setError(getString(R.string.checklist_error_category_exists));
            } else {
                category.setName(name);
                executorService.execute(() -> {
                    checklistDao.updateCategory(category);
                    loadChecklistData();
                });
                dialog.dismiss();
                return;
            }
            Toast.makeText(requireContext(), etListName.getError(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), R.string.checklist_toast_create_list_first, Toast.LENGTH_SHORT).show();
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
            etItemName.setError(null);

            if (ValidationUtils.isNullOrEmpty(itemName)) {
                etItemName.setError(getString(R.string.checklist_error_enter_item_name));
            } else if (!ValidationUtils.isValidDisplayName(itemName, MIN_ITEM_NAME_LENGTH, MAX_ITEM_NAME_LENGTH)) {
                etItemName.setError(getString(R.string.checklist_error_item_name_invalid));
            } else if (selectedPosition < 0) {
                Toast.makeText(requireContext(), R.string.checklist_toast_select_category, Toast.LENGTH_SHORT).show();
                return;
            } else if (ValidationUtils.isDuplicateName(itemName, getTaskNamesInCategory(currentCategories.get(selectedPosition).getId(), null))) {
                etItemName.setError(getString(R.string.checklist_error_item_exists));
            } else {
                int categoryId = currentCategories.get(selectedPosition).getId();
                executorService.execute(() -> {
                    ChecklistTask newTask = new ChecklistTask(categoryId, itemName);
                    checklistDao.insertTask(newTask);
                    loadChecklistData();
                });
                dialog.dismiss();
                return;
            }
            if (etItemName.getError() != null) {
                Toast.makeText(requireContext(), etItemName.getError(), Toast.LENGTH_SHORT).show();
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
                .setTitle(R.string.checklist_dialog_delete_title)
                .setMessage(R.string.checklist_dialog_delete_item_message)
                .setPositiveButton(R.string.common_delete, (dialog, which) -> {
                    executorService.execute(() -> {
                        checklistDao.deleteTask(task);
                        loadChecklistData();
                    });
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    @Override
    public void onTaskEdit(ChecklistTask task) {
        EditText input = new EditText(requireContext());
        input.setText(task.getName());
        input.setPadding(50, 50, 50, 50);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.checklist_dialog_edit_item_title)
                .setView(input)
                .setPositiveButton(R.string.common_update, null)
                .setNegativeButton(R.string.common_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String newName = input.getText().toString().trim();
                    input.setError(null);
                    if (ValidationUtils.isNullOrEmpty(newName)) {
                        input.setError(getString(R.string.checklist_error_name_empty));
                    } else if (!ValidationUtils.isValidDisplayName(newName, MIN_ITEM_NAME_LENGTH, MAX_ITEM_NAME_LENGTH)) {
                        input.setError(getString(R.string.checklist_error_item_name_invalid));
                    } else if (ValidationUtils.isDuplicateName(newName, getTaskNamesInCategory(task.getCategoryId(), task))) {
                        input.setError(getString(R.string.checklist_error_item_exists));
                    } else {
                        task.setName(newName);
                        executorService.execute(() -> {
                            checklistDao.updateTask(task);
                            loadChecklistData();
                        });
                        dialog.dismiss();
                        return;
                    }
                    Toast.makeText(requireContext(), input.getError(), Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }

    private List<String> getCategoryNamesExcept(@Nullable ChecklistCategory excludedCategory) {
        List<String> names = new ArrayList<>();
        for (ChecklistCategory category : currentCategories) {
            if (excludedCategory != null && category.getId() == excludedCategory.getId()) {
                continue;
            }
            names.add(category.getName());
        }
        return names;
    }

    private List<String> getTaskNamesInCategory(int categoryId, @Nullable ChecklistTask excludedTask) {
        List<String> names = new ArrayList<>();
        List<ChecklistTask> tasks = currentTaskMap.get(categoryId);
        if (tasks == null) {
            return names;
        }
        for (ChecklistTask existingTask : tasks) {
            if (excludedTask != null && existingTask.getId() == excludedTask.getId()) {
                continue;
            }
            names.add(existingTask.getName());
        }
        return names;
    }

}