package com.example.vs_project.ui.dashboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityDashboardBinding;
import com.example.vs_project.ui.scanner.FaceScannerActivity;

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;
    private String userRole = "";
    private String userName = "";
    private String orgId = "";

    
    private final ActivityResultLauncher<String> csvPickerLauncher = 
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                
                String fileName = uri.getLastPathSegment();
                Toast.makeText(this, "CSV Selected: " + fileName + "\nUploading to cloud...", Toast.LENGTH_LONG).show();
                
            } else {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            userRole = getIntent().getStringExtra("USER_ROLE");
            userName = getIntent().getStringExtra("USER_NAME");
            orgId = getIntent().getStringExtra("ORG_ID");
            
            if (userRole == null) userRole = "unknown";
            if (userName == null) userName = "User";
            if (orgId == null) orgId = "unknown";
        }

        if (userName == null || userName.equals("null") || userName.trim().isEmpty()) {
            userName = "User";
        }

        binding.tvWelcome.setText("Welcome, " + userName);
        binding.tvRole.setText("Role: " + userRole.toUpperCase() + " | Org: " + orgId);

        setupRoleBasedUI();
        fetchActiveSessions();


        

        if (binding.btnExamRegistration != null) {
            binding.btnExamRegistration.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, ExamRegistrationActivity.class);
                intent.putExtra("ORG_ID", orgId);
                intent.putExtra("USER_ID", getIntent().getStringExtra("USER_ID"));
                startActivity(intent);
            });
        }

        
        if (binding.btnViewTimetable != null) {
            binding.btnViewTimetable.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, TimetableActivity.class);
                intent.putExtra("ORG_ID", orgId);
                intent.putExtra("USER_ROLE", "student"); 
                intent.putExtra("USER_ID", getIntent().getStringExtra("USER_ID"));
                startActivity(intent);
            });
        }

        if (binding.btnLogout != null) {
            binding.btnLogout.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        getSharedPreferences("VisionSecureSession", MODE_PRIVATE).edit().clear().apply();
                        Intent intent = new Intent(DashboardActivity.this, com.example.vs_project.ui.auth.GetStartedActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    private void fetchActiveSessions() {
        if (orgId.equals("unknown")) return;
        
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("sessions")
          .whereEqualTo("org_id", orgId)
          .get()
          .addOnCompleteListener(task -> {
              if (task.isSuccessful() && task.getResult() != null) {
                  int count = task.getResult().size();
                  android.widget.Toast.makeText(this, "Found " + count + " active sessions for your Org!", android.widget.Toast.LENGTH_LONG).show();
                  
              }
          });
    }

    private void setupRoleBasedUI() {
        
    }
}
