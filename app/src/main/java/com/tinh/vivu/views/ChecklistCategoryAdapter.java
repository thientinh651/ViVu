package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.ChecklistCategory;
import com.tinh.vivu.models.ChecklistTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChecklistCategoryAdapter extends RecyclerView.Adapter<ChecklistCategoryAdapter.CategoryHolder> {

    private List<ChecklistCategory> categories;
    private Map<Integer, List<ChecklistTask>> taskMap = new HashMap<>();
    private ChecklistTaskAdapter.OnTaskActionListener taskListener;
    private OnCategoryActionListener categoryListener;

    // INTERFACE BẮT BUỘC ĐỂ TRÁNH LỖI "Cannot resolve symbol"
    public interface OnCategoryActionListener {
        void onCategoryReset(ChecklistCategory category);
        void onCategoryEdit(ChecklistCategory category);
        void onCategoryDelete(ChecklistCategory category);
    }

    // Constructor nhận 2 loại Listener (Task và Category)
    public ChecklistCategoryAdapter(ChecklistTaskAdapter.OnTaskActionListener taskListener, OnCategoryActionListener categoryListener) {
        this.taskListener = taskListener;
        this.categoryListener = categoryListener;
    }

    public void setData(List<ChecklistCategory> categories, Map<Integer, List<ChecklistTask>> taskMap) {
        this.categories = categories;
        this.taskMap = taskMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checklist_category, parent, false);
        return new CategoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, int position) {
        ChecklistCategory category = categories.get(position);
        holder.tvName.setText(category.getName().toUpperCase());

        // Bắt sự kiện bấm vào nút 3 chấm
        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), holder.btnMore);
            popup.getMenu().add(0, 0, 0, "Reset Danh Mục");
            popup.getMenu().add(0, 1, 1, "Sửa Tên");
            popup.getMenu().add(0, 2, 2, "Xóa Danh Mục");

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 0:
                        if (categoryListener != null) categoryListener.onCategoryReset(category);
                        return true;
                    case 1:
                        if (categoryListener != null) categoryListener.onCategoryEdit(category);
                        return true;
                    case 2:
                        if (categoryListener != null) categoryListener.onCategoryDelete(category);
                        return true;
                }
                return false;
            });
            popup.show();
        });

        // Load dữ liệu con (Task)
        List<ChecklistTask> tasks = taskMap.get(category.getId());
        if (tasks != null && !tasks.isEmpty()) {
            ChecklistTaskAdapter taskAdapter = new ChecklistTaskAdapter(tasks, taskListener);
            holder.rvTasks.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            holder.rvTasks.setAdapter(taskAdapter);
        } else {
            // Fix lỗi xóa phần tử cuối thì màn hình không cập nhật
            holder.rvTasks.setAdapter(null);
        }
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    static class CategoryHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView btnMore;
        RecyclerView rvTasks;

        public CategoryHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            btnMore = itemView.findViewById(R.id.btn_category_more);
            rvTasks = itemView.findViewById(R.id.rv_tasks);
        }
    }
}