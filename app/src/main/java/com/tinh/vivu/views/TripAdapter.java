package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinh.vivu.R;
import com.tinh.vivu.models.Trip;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripHolder> {

    private List<Trip> trips = new ArrayList<>();
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onDeleteClick(Trip trip);
        void onEditClick(Trip trip);
        void onTripClick(Trip trip);
    }

    public void setOnTripClickListener(OnTripClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TripHolder holder, int position) {
        Trip currentTrip = trips.get(position);

        holder.tvTripName.setText(currentTrip.getName());
        holder.tvTripStatus.setText(getLocalizedStatus(holder.itemView, currentTrip.getStatus()));

        String dateString = currentTrip.getStartDate();
        if (currentTrip.getEndDate() != null && !currentTrip.getEndDate().isEmpty()) {
            dateString += " - " + currentTrip.getEndDate();
        }
        holder.tvTripDate.setText(dateString);

        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedBudget = formatter.format(currentTrip.getTotalBudget());
        holder.tvTripBudget.setText(holder.itemView.getContext().getString(R.string.trip_budget_format, formattedBudget));


        holder.btnDeleteTrip.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(currentTrip);
        });


        holder.btnEditTrip.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(currentTrip);
        });


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTripClick(currentTrip);
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    public void setTrips(List<Trip> trips) {
        this.trips = trips;
        notifyDataSetChanged();
    }

    private String getLocalizedStatus(View view, String status) {
        if ("Ongoing".equalsIgnoreCase(status)) {
            return view.getContext().getString(R.string.trip_status_ongoing);
        }
        if ("Completed".equalsIgnoreCase(status)) {
            return view.getContext().getString(R.string.trip_status_completed);
        }
        return view.getContext().getString(R.string.trip_status_planning);
    }

    static class TripHolder extends RecyclerView.ViewHolder {
        private TextView tvTripName, tvTripStatus, tvTripDate, tvTripBudget;
        private ImageView btnDeleteTrip, btnEditTrip;

        public TripHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tv_trip_name);
            tvTripStatus = itemView.findViewById(R.id.tv_trip_status);
            tvTripDate = itemView.findViewById(R.id.tv_trip_date);
            tvTripBudget = itemView.findViewById(R.id.tv_trip_budget);
            btnDeleteTrip = itemView.findViewById(R.id.btn_delete_trip);
            btnEditTrip = itemView.findViewById(R.id.btn_edit_trip); // ÁNH XẠ
        }
    }
}