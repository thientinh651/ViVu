//package com.tinh.vivu.adapters;
package com.tinh.vivu.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tinh.vivu.R;
import com.tinh.vivu.models.JourneyLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JourneyLogAdapter extends RecyclerView.Adapter<JourneyLogAdapter.LogViewHolder> {

    private List<JourneyLog> logs = new ArrayList<>();
    private OnLogClickListener listener;

    public interface OnLogClickListener {
        void onDeleteClick(JourneyLog log);
        void onItemClick(JourneyLog log);
    }

    public void setOnLogClickListener(OnLogClickListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<JourneyLog> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journey_log, parent, false);
        return new LogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        JourneyLog log = logs.get(position);
        holder.tvTitle.setText(log.getTitle());
        holder.tvContent.setText(log.getContent());
        holder.tvLocation.setText(log.getLocationDisplay());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(log.getCreatedAt())));

        // Load ảnh (Glide)
        holder.imageLayout.removeAllViews();
        if (log.getImagePaths() != null && !log.getImagePaths().isEmpty()) {
            String[] paths = log.getImagePaths().split("\\|");
            for (String path : paths) {
                ImageView iv = new ImageView(holder.itemView.getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(200, 200);
                lp.setMargins(0, 0, 16, 0);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

                Glide.with(holder.itemView.getContext())
                        .load(path)
                        .placeholder(R.color.placeholder_gray)
                        .into(iv);

                holder.imageLayout.addView(iv);
            }
        }
    }

    @Override
    public int getItemCount() { return logs.size(); }

    class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvLocation, tvDate;
        LinearLayout imageLayout;
        View btnDelete;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.item_title);
            tvContent = itemView.findViewById(R.id.item_content);
            tvLocation = itemView.findViewById(R.id.item_location);
            tvDate = itemView.findViewById(R.id.item_date);
            imageLayout = itemView.findViewById(R.id.item_image_container);
            btnDelete = itemView.findViewById(R.id.btn_delete_log);

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(logs.get(getAdapterPosition()));
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(logs.get(getAdapterPosition()));
            });
        }
    }
}