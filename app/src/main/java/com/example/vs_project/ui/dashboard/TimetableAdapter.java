package com.example.vs_project.ui.dashboard;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vs_project.R;
import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.ViewHolder> {

    private List<TimetableSlot> slots;
    private String userRole;

    public TimetableAdapter(List<TimetableSlot> slots, String userRole) {
        this.slots = slots;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timetable_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimetableSlot slot = slots.get(position);
        
        holder.tvDate.setText(slot.date != null ? slot.date : "N/A");
        holder.tvTiming.setText(slot.timing != null ? slot.timing : "N/A");
        holder.tvSubject.setText(slot.subjectCode != null ? slot.subjectCode : "N/A");
        holder.tvHallNo.setText(slot.hallNo != null ? slot.hallNo : "N/A");
        holder.tvFacultyId.setText(slot.facultyId != null ? slot.facultyId : "N/A");
        
        String examMode = slot.examMode != null ? slot.examMode : "Offline";
        holder.tvExamMode.setText(examMode.substring(0, 1).toUpperCase() + examMode.substring(1).toLowerCase());
        
        if (examMode.equalsIgnoreCase("online")) {
            holder.tvExamMode.getBackground().setTint(Color.parseColor("#10B981")); 
        } else {
            holder.tvExamMode.getBackground().setTint(Color.parseColor("#3B82F6")); 
        }
        
        
        if ("student".equalsIgnoreCase(userRole)) {
            holder.llFacultyContainer.setVisibility(View.GONE);
            holder.llHallContainer.setVisibility(View.GONE);
            holder.tvExamMode.setVisibility(View.GONE);
        } else if ("faculty".equalsIgnoreCase(userRole) || "incharge".equalsIgnoreCase(userRole) || "invigilator".equalsIgnoreCase(userRole)) {
            holder.llFacultyContainer.setVisibility(View.GONE);
            holder.llHallContainer.setVisibility(View.VISIBLE);
            holder.tvExamMode.setVisibility(View.VISIBLE);
        } else {
            holder.llFacultyContainer.setVisibility(View.VISIBLE);
            holder.llHallContainer.setVisibility(View.VISIBLE);
            holder.tvExamMode.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTiming, tvSubject, tvHallNo, tvFacultyId, tvExamMode;
        LinearLayout llFacultyContainer, llHallContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTiming = itemView.findViewById(R.id.tvTiming);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvHallNo = itemView.findViewById(R.id.tvHallNo);
            tvFacultyId = itemView.findViewById(R.id.tvFacultyId);
            tvExamMode = itemView.findViewById(R.id.tvExamMode);
            llFacultyContainer = itemView.findViewById(R.id.llFacultyContainer);
            llHallContainer = itemView.findViewById(R.id.llHallContainer);
        }
    }
}
