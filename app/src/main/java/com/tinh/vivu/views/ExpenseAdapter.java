package com.tinh.vivu.views;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.Expense;
import com.tinh.vivu.models.ExpenseCategory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList = new ArrayList<>();
    private List<ExpenseCategory> categoryList = new ArrayList<>();
    private DecimalFormat formatter = new DecimalFormat("#,###");

    // Khai báo Interface để bắt sự kiện click
    private OnExpenseItemClickListener listener;

    public interface OnExpenseItemClickListener {
        void onExpenseClick(Expense expense);
    }

    public void setOnExpenseItemClickListener(OnExpenseItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Expense> expenses, List<ExpenseCategory> categories) {
        this.expenseList = expenses;
        this.categoryList = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);

        holder.tvNote.setText(expense.getNote() != null && !expense.getNote().isEmpty() ? expense.getNote() : "Khoản chi");
        holder.tvAmount.setText(formatter.format(expense.getAmount()) + " đ");
        holder.tvDate.setText(expense.getExpenseDate());

        // Tìm tên danh mục từ categoryId
        String categoryName = "Khác";
        for (ExpenseCategory cat : categoryList) {
            if (cat.getCategoryId() == expense.getCategoryId()) {
                categoryName = cat.getCategoryName();
                break;
            }
        }
        holder.tvCategory.setText(categoryName);

        // Đổi màu icon dựa trên tên danh mục (Demo)
        if (categoryName.toLowerCase().contains("xăng") || categoryName.toLowerCase().contains("di chuyển")) {
            holder.flIconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF0E0")));
            holder.ivIcon.setColorFilter(Color.parseColor("#FF8800"));
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_directions);
        } else if (categoryName.toLowerCase().contains("ăn")) {
            holder.flIconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50"));
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_manage);
        } else {
            holder.flIconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
            holder.ivIcon.setColorFilter(Color.parseColor("#2196F3"));
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        }

        // Bắt sự kiện click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenseList != null ? expenseList.size() : 0;
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvNote, tvAmount, tvDate, tvCategory;
        FrameLayout flIconBg;
        ImageView ivIcon;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNote = itemView.findViewById(R.id.tv_expense_note);
            tvAmount = itemView.findViewById(R.id.tv_expense_amount);
            tvDate = itemView.findViewById(R.id.tv_expense_date);
            tvCategory = itemView.findViewById(R.id.tv_expense_category);
            flIconBg = itemView.findViewById(R.id.fl_icon_bg);
            ivIcon = itemView.findViewById(R.id.iv_expense_icon);
        }
    }
}