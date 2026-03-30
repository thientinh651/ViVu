package com.tinh.vivu.views;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.ChecklistTask;

import java.util.List;

public class ChecklistTaskAdapter extends RecyclerView.Adapter<ChecklistTaskAdapter.TaskHolder> {

    private List<ChecklistTask> tasks;
    private OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskStatusChanged(ChecklistTask task, boolean isChecked);
        void onTaskDeleted(ChecklistTask task);
        void onTaskEdit(ChecklistTask task); // Thêm sự kiện Edit
    }

    public ChecklistTaskAdapter(List<ChecklistTask> tasks, OnTaskActionListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checklist_task, parent, false);
        return new TaskHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskHolder holder, int position) {
        ChecklistTask task = tasks.get(position);
        holder.tvName.setText(task.getName());

        // Gỡ listener cũ ra trước khi set trạng thái để tránh lỗi vòng lặp khi recycle view
        holder.cbStatus.setOnCheckedChangeListener(null);
        holder.cbStatus.setChecked(task.isCompleted());

        // Hiệu ứng gạch ngang chữ nếu đã hoàn thành (giống UI)
        if (task.isCompleted()) {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setTextColor(0xFF9E9E9E); // Màu xám
        } else {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvName.setTextColor(0xFF000000); // Màu đen
        }

        // Bắt sự kiện Check / Uncheck
        holder.cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTaskStatusChanged(task, isChecked);
            }
        });

        // Bắt sự kiện Xóa
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDeleted(task);
            }
        });

        // Bắt sự kiện Nhấn đúp (Double-Click) vào cả dòng để Sửa
        holder.itemView.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - holder.lastClickTime < 300) { // 300ms khoảng cách giữa 2 lần bấm
                if (listener != null) {
                    listener.onTaskEdit(task);
                }
            }
            holder.lastClickTime = clickTime;
        });
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class TaskHolder extends RecyclerView.ViewHolder {
        CheckBox cbStatus;
        TextView tvName;
        ImageView btnDelete;
        long lastClickTime = 0; // Biến lưu thời gian click để tính double-click

        public TaskHolder(@NonNull View itemView) {
            super(itemView);
            cbStatus = itemView.findViewById(R.id.cb_task_status);
            tvName = itemView.findViewById(R.id.tv_task_name);
            btnDelete = itemView.findViewById(R.id.btn_delete_task);
        }
    }
}