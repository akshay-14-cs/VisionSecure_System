package com.example.vs_project.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityInchargeDashboardBinding;
import com.example.vs_project.ui.scanner.FaceScannerActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.example.vs_project.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InchargeDashboardActivity extends AppCompatActivity {

    private ActivityInchargeDashboardBinding binding;
    private String orgId = "";
    private String userName = "";
    private String userId = "";
    private String department = "";
    
    private List<LogItem> examRegList = new ArrayList<>();
    private List<LogItem> faceRegList = new ArrayList<>();
    private List<String> autocompleteStudentNames = new ArrayList<>();
    private Map<String, String> studentNameToIdMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInchargeDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        com.example.vs_project.utils.ThemeUtils.setupSwitch(this, binding.switchTheme);

        if (getIntent() != null) {
            orgId = getIntent().getStringExtra("ORG_ID");
            userName = getIntent().getStringExtra("USER_NAME");
            userId = getIntent().getStringExtra("USER_ID");
        }

        if (userName == null || userName.equals("null") || userName.trim().isEmpty()) {
            userName = "Incharge";
        }

        
        if (userId != null && !userId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener(doc -> {
                if (doc.exists() && doc.contains("department")) {
                    department = doc.getString("department");
                    setupStatsListener();
                }
            });
        }

        binding.tvWelcomeIncharge.setText("Welcome, " + userName);

        binding.cardRegisterFace.setOnClickListener(v -> {
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_face, null);
            android.widget.AutoCompleteTextView actvStudent = dialogView.findViewById(R.id.actvStudent);
            
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, autocompleteStudentNames);
            actvStudent.setAdapter(adapter);

            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
                
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
            dialogView.findViewById(R.id.btnProceed).setOnClickListener(v1 -> {
                String selectedText = actvStudent.getText().toString().trim();
                if (selectedText.isEmpty()) {
                    android.widget.Toast.makeText(this, "Student ID/Name is required", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String targetStudentId = studentNameToIdMap.containsKey(selectedText) ? studentNameToIdMap.get(selectedText) : selectedText;
                
                String fullStudentId = orgId + "_" + targetStudentId;
                
                
                FirebaseFirestore.getInstance().collection("students").document(fullStudentId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            com.google.firebase.firestore.DocumentSnapshot doc = task.getResult();
                            String studentDept = doc.getString("department");
                            Boolean examRegistered = doc.getBoolean("exam_registered");
                            Boolean hasFaceRegistered = doc.getBoolean("has_face_registered");

                            if (studentDept == null || !studentDept.equalsIgnoreCase(department)) {
                                android.widget.Toast.makeText(this, "Student does not belong to your department.", android.widget.Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (examRegistered == null || !examRegistered) {
                                android.widget.Toast.makeText(this, "Student must complete exam registration before face registration.", android.widget.Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (hasFaceRegistered != null && hasFaceRegistered) {
                                android.widget.Toast.makeText(this, "Face is already registered for this student.", android.widget.Toast.LENGTH_LONG).show();
                                return;
                            }

                            dialog.dismiss();
                            Intent intent = new Intent(InchargeDashboardActivity.this, FaceScannerActivity.class);
                            intent.putExtra("SCANNER_MODE", "REGISTRATION");
                            intent.putExtra("ORG_ID", orgId);
                            intent.putExtra("USER_ID", targetStudentId);
                            startActivity(intent);
                        } else {
                            android.widget.Toast.makeText(this, "Student not registered in the system", android.widget.Toast.LENGTH_LONG).show();
                        }
                    });
            });
            dialog.show();
        });
        
        if (binding.cardViewStudents != null) {
            binding.cardViewStudents.setOnClickListener(v -> {
                Intent intent = new Intent(InchargeDashboardActivity.this, StudentDatabaseActivity.class);
                intent.putExtra("ORG_ID", orgId);
                intent.putExtra("USER_ROLE", "incharge");
                intent.putExtra("DEPARTMENT", department);
                startActivity(intent);
            });
        }

        binding.btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    
                    getSharedPreferences("VisionSecureSession", MODE_PRIVATE).edit().clear().apply();
                    
                    Intent intent = new Intent(InchargeDashboardActivity.this, com.example.vs_project.ui.auth.GetStartedActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setupStatsListener() {
        if (orgId == null || orgId.isEmpty() || department == null || department.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("students")
            .whereEqualTo("org_id", orgId)
            .whereEqualTo("department", department)
            .addSnapshotListener((value, error) -> {
                if (error != null || value == null) return;
                
                int examRegCount = 0;
                int faceRegCount = 0;
                
                examRegList.clear();
                faceRegList.clear();
                autocompleteStudentNames.clear();
                studentNameToIdMap.clear();
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
                
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                    Boolean isExamReg = doc.getBoolean("exam_registered");
                    Boolean hasFaceReg = doc.getBoolean("has_face_registered");
                    String stuId = doc.getString("student_id");
                    String stuName = doc.getString("name");
                    
                    if (stuId != null && stuName != null) {
                        String display = stuName + " (" + stuId + ")";
                        autocompleteStudentNames.add(display);
                        studentNameToIdMap.put(display, stuId);
                    }
                    
                    if (isExamReg != null && isExamReg) {
                        examRegCount++;
                        com.google.firebase.Timestamp ts = doc.getTimestamp("exam_registered_at");
                        String timeStr = ts != null ? sdf.format(ts.toDate()) : "N/A";
                        examRegList.add(new LogItem("Exam Registration Complete", "Student: " + stuName + " (ID: " + stuId + ")", timeStr, null));
                    }
                    if (hasFaceReg != null && hasFaceReg) {
                        faceRegCount++;
                        faceRegList.add(new LogItem("Face Registration Complete", "Student: " + stuName + " (ID: " + stuId + ")", "Completed", null));
                    }
                }
                
                if (binding.tvExamRegCount != null) binding.tvExamRegCount.setText(String.valueOf(examRegCount));
                if (binding.tvFaceRegCount != null) binding.tvFaceRegCount.setText(String.valueOf(faceRegCount));
                
                if (binding.cardExamReg != null) {
                    binding.cardExamReg.setOnClickListener(v -> showStudentsBottomSheet("Exam Registrations", examRegList));
                }
                if (binding.cardFaceReg != null) {
                    binding.cardFaceReg.setOnClickListener(v -> showStudentsBottomSheet("Face Registrations", faceRegList));
                }
            });
    }
    
    private void showStudentsBottomSheet(String title, List<LogItem> list) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.activity_live_exam_precheck, null); 
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 32);
        layout.addView(tvTitle);
        
        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(this);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rv.setAdapter(new LogAdapter(list, false, null));
        layout.addView(rv);
        
        dialog.setContentView(layout);
        dialog.show();
    }
}
