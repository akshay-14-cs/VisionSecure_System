package com.example.vs_project.ui.dashboard;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vs_project.R;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private List<LogItem> list;
    private boolean isSecurity;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(String title, String base64Image);
    }

    public LogAdapter(List<LogItem> list, boolean isSecurity, OnImageClickListener listener) {
        this.list = list;
        this.isSecurity = isSecurity;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogItem item = list.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvDetails.setText(item.details);
        holder.tvTime.setText(item.timestamp);

        if (isSecurity) {
            holder.tvTitle.setTextColor(Color.RED);
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            holder.ivIcon.setColorFilter(Color.RED);
            
            if (item.imageBase64 != null && !item.imageBase64.isEmpty()) {
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onImageClick(item.title, item.imageBase64);
                    }
                });
            } else {
                holder.itemView.setOnClickListener(null);
            }
        } else if (item.title != null && item.title.startsWith("Pending")) {
            holder.tvTitle.setTextColor(Color.parseColor("#F44336")); 
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_info);
            holder.ivIcon.setColorFilter(Color.parseColor("#F44336"));
            holder.itemView.setOnClickListener(null);
        } else {
            holder.tvTitle.setTextColor(Color.parseColor("#4CAF50")); 
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50"));
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvTime;
        ImageView ivIcon;
        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLogTitle);
            tvDetails = itemView.findViewById(R.id.tvLogDetails);
            tvTime = itemView.findViewById(R.id.tvLogTimestamp);
            ivIcon = itemView.findViewById(R.id.ivLogIcon);
        }
    }
}
