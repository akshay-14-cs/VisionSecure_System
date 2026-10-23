package com.example.vs_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityGetStartedBinding;

public class GetStartedActivity extends AppCompatActivity {

    private ActivityGetStartedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        android.content.SharedPreferences prefs = getSharedPreferences("VisionSecureSession", MODE_PRIVATE);
        if (prefs.getBoolean("IS_LOGGED_IN", false)) {
            String role = prefs.getString("USER_ROLE", "");
            String orgId = prefs.getString("ORG_ID", "");
            String name = prefs.getString("USER_NAME", "");
            String orgType = prefs.getString("ORG_TYPE", "");
            String userId = prefs.getString("USER_ID", "");
            
            
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            
            binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            
            binding.btnAdminLogin.setEnabled(false);
            binding.btnInchargeLogin.setEnabled(false);
            binding.btnStaffLogin.setEnabled(false);
            binding.btnStudentLogin.setEnabled(false);
            android.widget.Toast.makeText(this, "Validating session...", android.widget.Toast.LENGTH_SHORT).show();
            
            if (userId == null || userId.isEmpty()) {
                prefs.edit().clear().apply();
                Intent intent = new Intent(this, GetStartedActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return;
            }
            
            db.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Class<?> targetActivity;
                    if ("admin".equalsIgnoreCase(role)) {
                        targetActivity = com.example.vs_project.ui.dashboard.AdminDashboardActivity.class;
                    } else if ("incharge".equalsIgnoreCase(role)) {
                        targetActivity = com.example.vs_project.ui.dashboard.InchargeDashboardActivity.class;
                    } else if ("student".equalsIgnoreCase(role)) {
                        targetActivity = com.example.vs_project.ui.dashboard.DashboardActivity.class;
                    } else if ("invigilator".equalsIgnoreCase(role)) {
                        targetActivity = com.example.vs_project.ui.dashboard.InvigilatorDashboardActivity.class;
                    } else {
                        targetActivity = com.example.vs_project.ui.dashboard.FacultyDashboardActivity.class;
                    }
                    
                    Intent intent = new Intent(this, targetActivity);
                    intent.putExtra("USER_ROLE", role);
                    intent.putExtra("ORG_ID", orgId);
                    intent.putExtra("USER_NAME", name);
                    intent.putExtra("ORG_TYPE", orgType);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                    finish();
                } else {
                    
                    prefs.edit().clear().apply();
                    android.widget.Toast.makeText(this, "Session invalid. Please login again.", android.widget.Toast.LENGTH_LONG).show();
                    
                    
                    binding.btnAdminLogin.setEnabled(true);
                    binding.btnInchargeLogin.setEnabled(true);
                    binding.btnStaffLogin.setEnabled(true);
                    binding.btnStudentLogin.setEnabled(true);
                    
                    setupClickListeners();
                }
            });
            return;
        }

        binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnAdminLogin.setOnClickListener(v -> navigateToLogin("admin"));
        binding.btnInchargeLogin.setOnClickListener(v -> navigateToLogin("incharge"));
        binding.btnStaffLogin.setOnClickListener(v -> navigateToLogin("faculty"));
        binding.btnStudentLogin.setOnClickListener(v -> navigateToLogin("student"));

        binding.tvRegisterOrg.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrgRegistrationActivity.class);
            intent.putExtra("ORG_TYPE", "College");
            startActivity(intent);
        });
    }

    private void navigateToLogin(String expectedRole) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("EXPECTED_ROLE", expectedRole);
        startActivity(intent);
    }
}
