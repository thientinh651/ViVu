package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.RouteStop;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RouteStopAdapter extends RecyclerView.Adapter<RouteStopAdapter.StopHolder> {

    private List<RouteStop> stops = new ArrayList<>();
    private OnStopClickListener listener;

    public interface OnStopClickListener {
        void onDeleteClick(RouteStop stop);
        void onUpdateClick(RouteStop stop); // Thêm hàm cập nhật
    }

    public void setOnStopClickListener(OnStopClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StopHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_stop, parent, false);
        return new StopHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopHolder holder, int position) {
        RouteStop stop = stops.get(position);

        holder.tvOrder.setText(String.valueOf(stop.getOrderIndex()));
        holder.tvName.setText(stop.getLocationName());
        holder.tvExpArr.setText(stop.getExpectedArrival());
        holder.tvExpDep.setText(stop.getExpectedDeparture());

        // Hiển thị dữ liệu thực tế hiện có
        holder.tvActArr.setText(stop.getActualArrival().isEmpty() ? "--:-- --/--" : stop.getActualArrival());
        holder.tvActDep.setText(stop.getActualDeparture().isEmpty() ? "--:-- --/--" : stop.getActualDeparture());

        // Cập nhật trạng thái CheckBox (chặn sự kiện gán tự động)
        holder.cbArrived.setOnCheckedChangeListener(null);
        holder.cbDeparted.setOnCheckedChangeListener(null);

        holder.cbArrived.setChecked(!stop.getActualArrival().isEmpty());
        holder.cbDeparted.setChecked(!stop.getActualDeparture().isEmpty());

        // Xử lý tick "Đã đến"
        holder.cbArrived.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String currentTime = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date());
                stop.setActualArrival(currentTime);
                stop.setArrived(true);
            } else {
                stop.setActualArrival("");
                stop.setArrived(false);
            }
            if (listener != null) listener.onUpdateClick(stop);
            notifyItemChanged(position);
        });

        // Xử lý tick "Đã rời đi"
        holder.cbDeparted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String currentTime = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date());
                stop.setActualDeparture(currentTime);
            } else {
                stop.setActualDeparture("");
            }
            if (listener != null) listener.onUpdateClick(stop);
            notifyItemChanged(position);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(stop);
        });
    }

    @Override
    public int getItemCount() { return stops.size(); }

    public void setStops(List<RouteStop> stops) {
        this.stops = stops;
        notifyDataSetChanged();
    }

    static class StopHolder extends RecyclerView.ViewHolder {
        TextView tvOrder, tvName, tvExpArr, tvExpDep, tvActArr, tvActDep;
        CheckBox cbArrived, cbDeparted;
        ImageView btnDelete;

        public StopHolder(@NonNull View itemView) {
            super(itemView);
            tvOrder = itemView.findViewById(R.id.tv_order_index);
            tvName = itemView.findViewById(R.id.tv_stop_name);
            tvExpArr = itemView.findViewById(R.id.tv_exp_arrival);
            tvExpDep = itemView.findViewById(R.id.tv_exp_departure);
            tvActArr = itemView.findViewById(R.id.tv_actual_arrival);
            tvActDep = itemView.findViewById(R.id.tv_actual_departure);
            cbArrived = itemView.findViewById(R.id.cb_arrived);
            cbDeparted = itemView.findViewById(R.id.cb_departed);
            btnDelete = itemView.findViewById(R.id.btn_delete_stop);
        }
    }
}