package com.example.vs_project.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityFacultyDashboardBinding;
import com.example.vs_project.ui.scanner.FaceScannerActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.List;
import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class FacultyDashboardActivity extends AppCompatActivity {

    private ActivityFacultyDashboardBinding binding;
    private String orgId = "";
    private String userName = "";
    private String userId = "";
    private String department = "";
    private boolean isIncharge = false;

    private List<QueryDocumentSnapshot> timetableDocs = new ArrayList<>();
    private List<String> dates = new ArrayList<>();
    private List<String> timings = new ArrayList<>();
    private List<String> rooms = new ArrayList<>();

    private String selectedDate = "";
    private String selectedTiming = "";
    private String selectedRoom = "";
    private String selectedSubject = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFacultyDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        com.example.vs_project.utils.ThemeUtils.setupSwitch(this, binding.switchTheme);

        if (getIntent() != null) {
            orgId = getIntent().getStringExtra("ORG_ID");
            userName = getIntent().getStringExtra("USER_NAME");
            userId = getIntent().getStringExtra("USER_ID");
        }

        if (userName == null || userName.equals("null") || userName.trim().isEmpty()) {
            userName = "Faculty";
        }

        binding.tvWelcomeFaculty.setText("Welcome, " + userName);

        
        if (binding.spinnerExamMode != null) {
            String[] modes = {"Offline Exam", "Online Exam"};
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerExamMode.setAdapter(adapter);
        }

        
        if (userId != null && !userId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    if (doc.contains("department")) {
                        department = doc.getString("department");
                        binding.tvDepartment.setText("Dept: " + department);
                    } else {
                        binding.tvDepartment.setText("Role: Faculty");
                    }
                }
            });
        }
        if (binding.btnMyTimetable != null) {
            binding.btnMyTimetable.setOnClickListener(v -> {
                Intent intent = new Intent(FacultyDashboardActivity.this, TimetableActivity.class);
                intent.putExtra("ORG_ID", orgId);
                intent.putExtra("USER_ROLE", "faculty");
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            });
        }
        
        if (binding.btnViewStudents != null) {
            binding.btnViewStudents.setOnClickListener(v -> {
                Intent intent = new Intent(FacultyDashboardActivity.this, StudentDatabaseActivity.class);
                intent.putExtra("ORG_ID", orgId);
                intent.putExtra("USER_ROLE", "faculty");
                intent.putExtra("DEPARTMENT", department);
                startActivity(intent);
            });
        }

        binding.btnSuperviseExam.setOnClickListener(v -> {
            if (selectedDate.isEmpty() || selectedTiming.isEmpty() || selectedRoom.isEmpty()) {
                Toast.makeText(this, "Please wait or select Date, Timing and Room", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String examMode = binding.spinnerExamMode.getSelectedItem().toString();
            Toast.makeText(this, "Starting " + examMode + " Session: " + selectedSubject, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(FacultyDashboardActivity.this, FaceScannerActivity.class);
            if ("Online Exam".equals(examMode)) {
                intent.putExtra("SCANNER_MODE", "QR_SCAN");
            } else {
                intent.putExtra("SCANNER_MODE", "VERIFICATION");
            }
            intent.putExtra("ORG_ID", orgId);
            intent.putExtra("HALL_NO", selectedRoom);
            intent.putExtra("SUBJECT_CODE", selectedSubject);
            startActivity(intent);
        });

        fetchTimetable();

        setupSpinners();

        binding.btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    
                    getSharedPreferences("VisionSecureSession", MODE_PRIVATE).edit().clear().apply();
                    
                    Intent intent = new Intent(FacultyDashboardActivity.this, com.example.vs_project.ui.auth.GetStartedActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        
        com.example.vs_project.databinding.LayoutBottomNavigationBinding navBinding = com.example.vs_project.databinding.LayoutBottomNavigationBinding.bind(binding.getRoot());

        if (navBinding.bottomNavigation != null) {
            navBinding.bottomNavigation.setSelectedItemId(com.example.vs_project.R.id.nav_home);
            navBinding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == com.example.vs_project.R.id.nav_home) {
                    return true;
                } else if (itemId == com.example.vs_project.R.id.nav_students) {
                    Intent intent = new Intent(FacultyDashboardActivity.this, StudentDatabaseActivity.class);
                    intent.putExtra("ORG_ID", orgId);
                    intent.putExtra("USER_ROLE", "faculty");
                    intent.putExtra("DEPARTMENT", department);
                    startActivity(intent);
                    return false;
                } else if (itemId == com.example.vs_project.R.id.nav_analytics) {
                    Toast.makeText(this, "Analytics available to Admins only.", Toast.LENGTH_SHORT).show();
                    return false;
                } else if (itemId == com.example.vs_project.R.id.nav_more) {
                    Toast.makeText(this, "More / Settings coming soon", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return false;
            });
        }

        if (navBinding.fabRegistry != null) {
            navBinding.fabRegistry.setOnClickListener(v -> {
                binding.btnSuperviseExam.performClick();
            });
        }
    }

    private void setupSpinners() {
        if (binding.spinnerDate != null) {
            binding.spinnerDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedDate = dates.get(position);
                    updateTimingsSpinner();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        if (binding.spinnerTiming != null) {
            binding.spinnerTiming.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedTiming = timings.get(position);
                    updateRoomsSpinner();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        if (binding.spinnerRoom != null) {
            binding.spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedRoom = rooms.get(position);
                    updateExamDetails();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void fetchTimetable() {
        if(orgId == null || orgId.isEmpty()) return;
        
        FirebaseFirestore.getInstance().collection("timetable").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                timetableDocs.clear();
                dates.clear();
                
                String rawUserId = userId;
                if (!orgId.isEmpty() && rawUserId.startsWith(orgId + "_")) {
                    rawUserId = rawUserId.substring(orgId.length() + 1);
                } else if (!orgId.isEmpty() && rawUserId.startsWith(orgId + "-")) {
                    rawUserId = rawUserId.substring(orgId.length() + 1);
                }

                for (QueryDocumentSnapshot document : task.getResult()) {
                    
                    String docOrgId = document.getString("org_id");
                    if (docOrgId != null && !docOrgId.equals(orgId)) continue;
                    
                    
                    String docFacId = document.getString("faculty_id");
                    if (docFacId == null || !docFacId.equals(rawUserId)) continue;
                    
                    timetableDocs.add(document);
                    String d = document.getString("date");
                    if (d != null && !dates.contains(d)) {
                        dates.add(d);
                    }
                }
                
                if (binding.spinnerDate != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerDate.setAdapter(adapter);
                    if(!dates.isEmpty()) {
                        selectedDate = dates.get(0);
                        updateTimingsSpinner();
                    } else {
                        binding.tvExamDetails.setText("You have no scheduled exams.");
                    }
                }
            } else {
                Toast.makeText(this, "Failed to load timetable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTimingsSpinner() {
        timings.clear();
        for (QueryDocumentSnapshot doc : timetableDocs) {
            if (selectedDate.equals(doc.getString("date"))) {
                String t = doc.getString("timing");
                if (t != null && !timings.contains(t)) {
                    timings.add(t);
                }
            }
        }
        if (binding.spinnerTiming != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timings);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerTiming.setAdapter(adapter);
            if(!timings.isEmpty()) {
                selectedTiming = timings.get(0);
                updateRoomsSpinner();
            } else {
                selectedTiming = "";
                updateRoomsSpinner();
            }
        }
    }

    private void updateRoomsSpinner() {
        rooms.clear();
        for (QueryDocumentSnapshot doc : timetableDocs) {
            if (selectedDate.equals(doc.getString("date")) && selectedTiming.equals(doc.getString("timing"))) {
                String r = doc.getString("hall_no");
                if (r != null && !rooms.contains(r)) {
                    rooms.add(r);
                }
            }
        }
        if (binding.spinnerRoom != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rooms);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerRoom.setAdapter(adapter);
            if(!rooms.isEmpty()) {
                selectedRoom = rooms.get(0);
                updateExamDetails();
            } else {
                selectedRoom = "";
                if (binding.tvExamDetails != null) binding.tvExamDetails.setText("No exams found for this selection.");
            }
        }
    }

    private void updateExamDetails() {
        for (QueryDocumentSnapshot doc : timetableDocs) {
            if (selectedDate.equals(doc.getString("date")) && 
                selectedTiming.equals(doc.getString("timing")) && 
                selectedRoom.equals(doc.getString("hall_no"))) {
                
                selectedSubject = doc.getString("subject_code");
                String fac = doc.getString("faculty_id");
                String mode = doc.getString("exam_mode");
                
                String details = "Subject: " + selectedSubject + "\n" +
                                 "Faculty: " + fac + "\n" +
                                 "Mode: " + mode;
                if (binding.tvExamDetails != null) binding.tvExamDetails.setText(details);
                return;
            }
        }
        if (binding.tvExamDetails != null) binding.tvExamDetails.setText("");
    }


}
