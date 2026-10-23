package com.example.vs_project.ui.dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vs_project.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TimetableActivity extends AppCompatActivity {

    private RecyclerView rvTimetable;
    private TimetableAdapter adapter;
    private List<TimetableSlot> slotList;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private String orgId = "";
    private String userRole = "";
    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        rvTimetable = findViewById(R.id.rvTimetable);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        slotList = new ArrayList<>();

        if (getIntent() != null) {
            orgId = getIntent().getStringExtra("ORG_ID") != null ? getIntent().getStringExtra("ORG_ID") : "";
            userRole = getIntent().getStringExtra("USER_ROLE") != null ? getIntent().getStringExtra("USER_ROLE") : "";
            userId = getIntent().getStringExtra("USER_ID") != null ? getIntent().getStringExtra("USER_ID") : "";
        }

        adapter = new TimetableAdapter(slotList, userRole);
        rvTimetable.setLayoutManager(new LinearLayoutManager(this));
        rvTimetable.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchTimetable();
    }

    private void fetchTimetable() {
        if (orgId.isEmpty()) {
            Toast.makeText(this, "Organization ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvTimetable.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);

        Query query = db.collection("timetable").whereEqualTo("org_id", orgId);

        
        
        
        String searchUserId = userId;
        if (!orgId.isEmpty() && searchUserId.startsWith(orgId + "_")) {
            searchUserId = searchUserId.substring(orgId.length() + 1);
        } else if (!orgId.isEmpty() && searchUserId.startsWith(orgId + "-")) {
            searchUserId = searchUserId.substring(orgId.length() + 1);
        }

        if ("faculty".equalsIgnoreCase(userRole) || "incharge".equalsIgnoreCase(userRole) || "invigilator".equalsIgnoreCase(userRole)) {
            query = query.whereEqualTo("faculty_id", searchUserId);
        }

        
        
        query.get()
             .addOnCompleteListener(task -> {
                 if (task.isSuccessful() && task.getResult() != null) {
                     List<TimetableSlot> allFetchedSlots = new ArrayList<>();
                     for (QueryDocumentSnapshot doc : task.getResult()) {
                         TimetableSlot slot = doc.toObject(TimetableSlot.class);
                         allFetchedSlots.add(slot);
                     }

                     if ("student".equalsIgnoreCase(userRole)) {
                         
                         String fullUserId = userId.startsWith(orgId + "_") ? userId : orgId + "_" + userId;
                         db.collection("students").document(fullUserId).get().addOnCompleteListener(studentTask -> {
                             progressBar.setVisibility(View.GONE);
                             if (studentTask.isSuccessful() && studentTask.getResult() != null && studentTask.getResult().exists()) {
                                 
                                 String regStr = studentTask.getResult().getString("courses");
                                 String backStr = studentTask.getResult().getString("backlog_courses");
                                 
                                 java.util.Set<String> mySubjects = new java.util.HashSet<>();
                                 if (regStr != null) {
                                     for (String s : regStr.split(",")) {
                                         if (!s.trim().isEmpty()) mySubjects.add(s.trim());
                                     }
                                 }
                                 if (backStr != null) {
                                     for (String s : backStr.split(",")) {
                                         if (!s.trim().isEmpty()) mySubjects.add(s.trim());
                                     }
                                 }
                                 
                                 
                                 List<String> registeredExams = (List<String>) studentTask.getResult().get("registered_exams");
                                 if (registeredExams != null) {
                                     for (String s : registeredExams) {
                                         if (s != null && !s.trim().isEmpty()) mySubjects.add(s.trim());
                                     }
                                 }
                                 
                                 slotList.clear();
                                 java.util.Set<String> addedSignatures = new java.util.HashSet<>();
                                 for (TimetableSlot slot : allFetchedSlots) {
                                     if (slot.subjectCode != null && mySubjects.contains(slot.subjectCode.trim())) {
                                         String signature = slot.subjectCode.trim() + "_" + slot.date + "_" + slot.timing;
                                         if (!addedSignatures.contains(signature)) {
                                             slotList.add(slot);
                                             addedSignatures.add(signature);
                                         }
                                     }
                                 }
                                 sortAndDisplayTimetable();
                             } else {
                                 
                                 slotList.clear();
                                 sortAndDisplayTimetable();
                             }
                         });
                     } else {
                         progressBar.setVisibility(View.GONE);
                         slotList.clear();
                         slotList.addAll(allFetchedSlots);
                         sortAndDisplayTimetable();
                     }
                 } else {
                     progressBar.setVisibility(View.GONE);
                     llEmptyState.setVisibility(View.VISIBLE);
                 }
             });
    }
    private void sortAndDisplayTimetable() {
        java.util.Collections.sort(slotList, (s1, s2) -> {
            String d1 = s1.date != null ? s1.date : "";
            String d2 = s2.date != null ? s2.date : "";
            int dateCmp = d1.compareTo(d2);
            if (dateCmp != 0) return dateCmp;
            
            String t1 = s1.timing != null ? s1.timing : "";
            String t2 = s2.timing != null ? s2.timing : "";
            return t1.compareTo(t2);
        });

        adapter.notifyDataSetChanged();
        if (slotList.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvTimetable.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvTimetable.setVisibility(View.VISIBLE);
        }
    }
}
