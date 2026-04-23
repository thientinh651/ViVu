package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
        void onUpdateClick(RouteStop stop);
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

        String emptyTimePlaceholder = holder.itemView.getContext().getString(R.string.route_stop_actual_time_placeholder);
        holder.tvActArr.setText(stop.getActualArrival().isEmpty() ? emptyTimePlaceholder : stop.getActualArrival());
        holder.tvActDep.setText(stop.getActualDeparture().isEmpty() ? emptyTimePlaceholder : stop.getActualDeparture());

        // Reset listener để tránh bị gọi đè khi scroll
        holder.cbArrived.setOnCheckedChangeListener(null);
        holder.cbDeparted.setOnCheckedChangeListener(null);

        holder.cbArrived.setChecked(!stop.getActualArrival().isEmpty());
        holder.cbDeparted.setChecked(!stop.getActualDeparture().isEmpty());

        // Xử lý tick "Đã đến"
        holder.cbArrived.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String currentTime = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date());
                stop.setActualArrival(currentTime);
                stop.setArrived(true);
            } else {
                // Nếu hủy "Đã đến" thì cũng phải hủy luôn "Đã rời đi" vì logic không cho phép rời mà chưa đến
                stop.setActualArrival("");
                stop.setActualDeparture("");
                stop.setArrived(false);
                holder.cbDeparted.setChecked(false);
            }
            updateStop(stop);
            notifyItemChanged(position);
        });

        // Xử lý tick "Đã rời đi" - RÀNG BUỘC: PHẢI ĐẾN MỚI ĐƯỢC RỜI
        holder.cbDeparted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (stop.getActualArrival().isEmpty()) {
                    // Nếu chưa check Đã đến mà đòi check Đã rời
                    Toast.makeText(buttonView.getContext(), buttonView.getContext().getString(R.string.route_stop_toast_arrive_first), Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false);
                    return;
                }
                String currentTime = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date());
                stop.setActualDeparture(currentTime);
            } else {
                stop.setActualDeparture("");
            }
            updateStop(stop);
            notifyItemChanged(position);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(stop);
        });
    }

    private void updateStop(RouteStop stop) {
        if (listener != null) listener.onUpdateClick(stop);
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