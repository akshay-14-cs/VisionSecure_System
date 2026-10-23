package com.example.vs_project.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.R;
import com.example.vs_project.databinding.ActivityExamRegistrationBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ExamRegistrationActivity extends AppCompatActivity {

    private ActivityExamRegistrationBinding binding;
    private String orgId = "";
    private String userId = ""; 
    private java.util.List<String> selectedCourses = new java.util.ArrayList<>();
    private boolean isAlreadyRegistered = false;

    
    private static final Map<String, String> COURSE_NAMES = new HashMap<>();
    static {
        COURSE_NAMES.put("CM41205", "JAVA PROGRAMMING");
        COURSE_NAMES.put("CM41206", "SOFTWARE ENGINEERING AND TESTING");
        COURSE_NAMES.put("CM41207", "CAPSTONE PROJECT");
        COURSE_NAMES.put("CM51203", "MOBILE APPLICATION DEVELOPMENT");
        COURSE_NAMES.put("CM51205", "CYBER SECURITY");
        COURSE_NAMES.put("CM51207", "CLOUD COMPUTING");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            if (getIntent().hasExtra("ORG_ID")) orgId = getIntent().getStringExtra("ORG_ID");
            if (getIntent().hasExtra("USER_ID")) userId = getIntent().getStringExtra("USER_ID");
        }

        
        if (userId == null || userId.isEmpty()) {
            userId = getSharedPreferences("VisionSecureSession", MODE_PRIVATE).getString("USER_ID", "S101");
            orgId = getSharedPreferences("VisionSecureSession", MODE_PRIVATE).getString("ORG_ID", "default_org");
        }

        binding.tvStudentDetails.setText("Student ID: " + userId);
        loadStudentData();

        binding.btnConfirmRegistration.setOnClickListener(v -> confirmRegistration());
    }

    private void confirmRegistration() {
        if (selectedCourses.isEmpty()) {
            Toast.makeText(this, "Please select at least one course.", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Registration")
            .setMessage("Are you sure you want to submit your exam registration? You will not be able to change it after confirmation.")
            .setPositiveButton("Confirm", (dialog, which) -> executeRegistrationTransaction())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void executeRegistrationTransaction() {
        binding.btnConfirmRegistration.setEnabled(false);
        binding.btnConfirmRegistration.setText("Saving...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String fullUserId = userId.startsWith(orgId + "_") ? userId : orgId + "_" + userId;
        com.google.firebase.firestore.DocumentReference docRef = db.collection("students").document(fullUserId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            if (snapshot.exists() && snapshot.getBoolean("exam_registered") != null && snapshot.getBoolean("exam_registered")) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Registration already completed.", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
            
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("exam_registered", true);
            data.put("registered_exams", selectedCourses);
            data.put("exam_registered_at", com.google.firebase.firestore.FieldValue.serverTimestamp());
            
            transaction.set(docRef, data, com.google.firebase.firestore.SetOptions.merge());
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Exam Registration Confirmed!", Toast.LENGTH_LONG).show();
            binding.btnConfirmRegistration.setText("Exam registration has already been completed and cannot be changed.");
            isAlreadyRegistered = true;
            
            disableCheckboxes(binding.llCoursesContainer);
            disableCheckboxes(binding.llBacklogContainer);
        }).addOnFailureListener(e -> {
            binding.btnConfirmRegistration.setEnabled(true);
            binding.btnConfirmRegistration.setText("Confirm Registration");
            Toast.makeText(this, "Failed to register exams: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void disableCheckboxes(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            android.widget.CheckBox cb = child.findViewById(R.id.cbSelect);
            if (cb != null) {
                cb.setEnabled(false);
            }
        }
    }

    private void loadStudentData() {
        if (orgId.isEmpty() || userId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String fullUserId = userId.startsWith(orgId + "_") ? userId : orgId + "_" + userId;
        db.collection("students").document(fullUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        String name = doc.getString("name");
                        String coursesStr = doc.getString("courses");
                        String backlogStr = doc.getString("backlog_courses");

                        if (name != null) {
                            binding.tvStudentDetails.setText("Student: " + name + " (" + userId + ")");
                        }

                        Boolean examRegistered = doc.getBoolean("exam_registered");
                        if (examRegistered != null && examRegistered) {
                            isAlreadyRegistered = true;
                            binding.btnConfirmRegistration.setEnabled(false);
                            binding.btnConfirmRegistration.setText("Exam registration has already been completed and cannot be changed.");
                            java.util.List<String> exams = (java.util.List<String>) doc.get("registered_exams");
                            if (exams != null) {
                                selectedCourses.clear();
                                selectedCourses.addAll(exams);
                            }
                        }

                        populateCourses(binding.llCoursesContainer, coursesStr, true);
                        populateCourses(binding.llBacklogContainer, backlogStr, false);

                    } else {
                        Toast.makeText(this, "Failed to load student data", Toast.LENGTH_SHORT).show();
                        binding.tvStudentDetails.setText("Error loading data");
                    }
                });
    }

    private void populateCourses(LinearLayout container, String coursesString, boolean isRegular) {
        container.removeAllViews();
        
        if (coursesString == null || coursesString.trim().isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No courses found.");
            emptyTv.setPadding(16, 16, 16, 16);
            container.addView(emptyTv);
            return;
        }

        String[] courses = coursesString.split(",");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String course : courses) {
            String code = course.trim();
            if (code.isEmpty()) continue;

            View rowView = inflater.inflate(R.layout.item_course_row, container, false);
            
            TextView tvCode = rowView.findViewById(R.id.tvCourseCode);
            TextView tvName = rowView.findViewById(R.id.tvCourseName);
            TextView tvStatus = rowView.findViewById(R.id.tvStatus);
            android.widget.CheckBox cbSelect = rowView.findViewById(R.id.cbSelect);

            tvCode.setText(code);
            tvName.setText(COURSE_NAMES.containsKey(code) ? COURSE_NAMES.get(code) : "Elective/Special Course");
            
            cbSelect.setEnabled(!isAlreadyRegistered);
            if (selectedCourses.contains(code)) {
                cbSelect.setChecked(true);
            }
            
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedCourses.contains(code)) selectedCourses.add(code);
                } else {
                    selectedCourses.remove(code);
                }
            });

            if (isRegular) {
                tvStatus.setText("Regular");
                tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"));
                tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#1A10B981"));
            } else {
                tvStatus.setText("Backlog");
                tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#1AEF4444"));
            }

            container.addView(rowView);
            
            
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
            container.addView(divider);
        }
    }
}
