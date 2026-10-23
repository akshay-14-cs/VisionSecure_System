package com.example.vs_project.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vs_project.R;
import com.example.vs_project.databinding.ActivityLiveExamPrecheckBinding;
import com.example.vs_project.ui.scanner.FaceScannerActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LiveExamPreCheckActivity extends AppCompatActivity {

    private ActivityLiveExamPrecheckBinding binding;
    private FirebaseFirestore db;
    private String orgId = "";

    private List<RegistryStudent> allStudents = new ArrayList<>();
    private List<RegistryStudent> filteredStudents = new ArrayList<>();
    private StackAdapter adapter;
    private int currentTab = 0; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveExamPrecheckBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null && getIntent().hasExtra("ORG_ID")) {
            orgId = getIntent().getStringExtra("ORG_ID");
        }

        db = FirebaseFirestore.getInstance();

        binding.btnCancelExam.setOnClickListener(v -> finish());
        binding.btnStartExam.setOnClickListener(v -> {
            Intent intent = new Intent(this, FaceScannerActivity.class);
            intent.putExtra("ORG_ID", orgId);
            startActivity(intent);
            finish();
        });

        setupRecyclerView();
        setupTabs();
        listenToLiveRegistry();
    }

    private void setupRecyclerView() {
        adapter = new StackAdapter();
        binding.rvStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStudents.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                applyFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void listenToLiveRegistry() {
        if (orgId == null || orgId.isEmpty()) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        
        db.collection("students")
            .whereEqualTo("org_id", orgId)
            .whereEqualTo("exam_registered", true)
            .addSnapshotListener((value, error) -> {
                binding.progressBar.setVisibility(View.GONE);
                
                if (error != null) {
                    Toast.makeText(this, "Failed to load live registry.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null) {
                    allStudents.clear();
                    int pendingCount = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        String name = doc.getString("name");
                        String stuId = doc.getString("student_id");
                        Boolean hasFace = doc.getBoolean("has_face_registered");
                        boolean isReady = (hasFace != null && hasFace);
                        com.google.firebase.Timestamp ts = doc.getTimestamp("exam_registered_at");
                        
                        if (!isReady) pendingCount++;

                        RegistryStudent student = new RegistryStudent(
                                name != null ? name : "Unknown",
                                stuId != null ? stuId : "No ID",
                                isReady
                        );
                        student.registeredAt = (ts != null) ? ts.getSeconds() : 0L;
                        allStudents.add(student);
                    }
                    
                    
                    allStudents.sort((s1, s2) -> Long.compare(s2.registeredAt, s1.registeredAt));
                    
                    updateValidationStatus(pendingCount);
                    applyFilter();
                }
            });
    }

    private void applyFilter() {
        filteredStudents.clear();
        for (RegistryStudent s : allStudents) {
            if (currentTab == 0) {
                filteredStudents.add(s);
            } else if (currentTab == 1 && s.isReady) {
                filteredStudents.add(s);
            } else if (currentTab == 2 && !s.isReady) {
                filteredStudents.add(s);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateValidationStatus(int pendingCount) {
        if (allStudents.isEmpty()) {
            binding.tvExamStatus.setText("Waiting for students to register...");
            binding.tvExamStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.textSecondary));
            binding.btnStartExam.setEnabled(false);
            binding.btnStartExam.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        } else if (pendingCount == 0) {
            binding.tvExamStatus.setText("Validation Passed! All " + allStudents.size() + " students are ready.");
            binding.tvExamStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor));
            binding.btnStartExam.setEnabled(true);
            binding.btnStartExam.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)));
        } else {
            binding.tvExamStatus.setText("Validation Failed: " + pendingCount + " students pending face registration.");
            binding.tvExamStatus.setTextColor(android.graphics.Color.parseColor("#EF5350"));
            binding.btnStartExam.setEnabled(false);
            binding.btnStartExam.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        }
    }

    class RegistryStudent {
        String name, id;
        boolean isReady;
        long registeredAt;

        RegistryStudent(String name, String id, boolean isReady) {
            this.name = name;
            this.id = id;
            this.isReady = isReady;
        }
    }

    class StackAdapter extends RecyclerView.Adapter<StackAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_live_registry_student, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RegistryStudent s = filteredStudents.get(position);
            holder.tvName.setText(s.name);
            holder.tvId.setText("ID: " + s.id);

            if (s.isReady) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_input_add); 
                holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#10B981")); 
                holder.chipStatus.setText("Ready");
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
            } else {
                holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#EF5350")); 
                holder.chipStatus.setText("Action Pending");
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
            }
        }

        @Override
        public int getItemCount() {
            return filteredStudents.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName, tvId;
            com.google.android.material.chip.Chip chipStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivStatusIcon);
                tvName = itemView.findViewById(R.id.tvStudentName);
                tvId = itemView.findViewById(R.id.tvStudentId);
                chipStatus = itemView.findViewById(R.id.chipStatus);
            }
        }
    }
}
