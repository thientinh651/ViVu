package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.MaintenanceHistoryItem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceHistoryAdapter extends RecyclerView.Adapter<MaintenanceHistoryAdapter.MaintenanceViewHolder> {
    private final List<MaintenanceHistoryItem> historyItems = new ArrayList<>();
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public void setItems(List<MaintenanceHistoryItem> items) {
        historyItems.clear();
        if (items != null) {
            historyItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MaintenanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maintenance_history, parent, false);
        return new MaintenanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaintenanceViewHolder holder, int position) {
        MaintenanceHistoryItem item = historyItems.get(position);
        holder.tvType.setText(item.getTypeName());
        holder.tvMeta.setText(item.getDate() + " • " + item.getOdoAtMaint() + " km");
        holder.tvCost.setText(formatter.format(item.getCost()) + " đ");

        if (item.getNote() == null || item.getNote().trim().isEmpty()) {
            holder.tvNote.setVisibility(View.GONE);
        } else {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(item.getNote());
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    static class MaintenanceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvType;
        private final TextView tvMeta;
        private final TextView tvNote;
        private final TextView tvCost;

        public MaintenanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_history_type);
            tvMeta = itemView.findViewById(R.id.tv_history_meta);
            tvNote = itemView.findViewById(R.id.tv_history_note);
            tvCost = itemView.findViewById(R.id.tv_history_cost);
        }
    }
}
