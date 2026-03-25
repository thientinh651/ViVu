package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.RouteStop;

import java.util.ArrayList;
import java.util.List;

public class RouteStopAdapter extends RecyclerView.Adapter<RouteStopAdapter.StopHolder> {

    // ĐỔI TỪ List<String> SANG List<RouteStop> ĐỂ CHỨA DỮ LIỆU THẬT
    private List<RouteStop> stops = new ArrayList<>();
    private OnStopClickListener listener;

    public interface OnStopClickListener {
        void onDeleteClick(RouteStop stop);
    }

    public void setOnStopClickListener(OnStopClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StopHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_stop, parent, false);
        return new StopHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StopHolder holder, int position) {
        RouteStop currentStop = stops.get(position);

        // Đổ dữ liệu thật từ Model lên Giao diện
        holder.tvOrder.setText(String.valueOf(currentStop.getOrderNumber()));
        holder.tvLocationName.setText(currentStop.getLocationName());

        // Gắn thêm chữ " km" ở Adapter (UI) thay vì lưu trong Database
        holder.tvDistance.setText(currentStop.getDistance() + " km");

        // Bắt sự kiện xóa
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentStop);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stops.size();
    }

    public void setStops(List<RouteStop> stops) {
        this.stops = stops;
        notifyDataSetChanged();
    }

    static class StopHolder extends RecyclerView.ViewHolder {
        private TextView tvOrder, tvLocationName, tvDistance;
        private ImageView btnDelete;

        public StopHolder(@NonNull View itemView) {
            super(itemView);
            tvOrder = itemView.findViewById(R.id.tv_order);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            btnDelete = itemView.findViewById(R.id.btn_delete_stop);
        }
    }
}