package com.example.vs_project.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import com.example.vs_project.R;
import com.example.vs_project.databinding.ActivityAdminDashboardBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private String orgId = "";
    private String userName = "";
    private String orgType = "";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = FirebaseFirestore.getInstance();
        com.example.vs_project.utils.ThemeUtils.setupSwitch(this, binding.switchTheme);

        if (getIntent() != null) {
            orgId = getIntent().getStringExtra("ORG_ID");
            userName = getIntent().getStringExtra("USER_NAME");
            orgType = getIntent().getStringExtra("ORG_TYPE");
        }

        if (userName == null || userName.equals("null") || userName.trim().isEmpty()) {
            userName = "Organization Admin";
        }

        binding.tvWelcomeAdmin.setText("Welcome, " + userName);
        binding.tvOrgId.setText("Org ID: " + orgId);

        
        Intent serviceIntent = new Intent(this, IntruderAlertService.class);
        serviceIntent.putExtra("ORG_ID", orgId);
        startService(serviceIntent);

        
        binding.btnMenu.setOnClickListener(v -> {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        });

        
        if (binding.navigationView.getHeaderCount() > 0) {
            View headerView = binding.navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvNavName = headerView.findViewById(R.id.tvNavHeaderName);
                TextView tvNavOrg = headerView.findViewById(R.id.tvNavHeaderOrg);
                if (tvNavName != null) tvNavName.setText(userName);
                if (tvNavOrg != null) tvNavOrg.setText("Org ID: " + orgId);
            }
        }

        
        binding.navigationView.setNavigationItemSelectedListener(item -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            
            if (id == R.id.nav_drawer_home) {
                
                return true;
            } else if (id == R.id.nav_upload_students) {
                launchUploadActivity("STUDENTS");
            } else if (id == R.id.nav_upload_staff) {
                launchUploadActivity("STAFF");
            } else if (id == R.id.nav_upload_timetable) {
                launchUploadActivity("TIMETABLE");
            } else if (id == R.id.nav_db_students) {
                launchActivity(StudentDatabaseActivity.class);
            } else if (id == R.id.nav_db_staff) {
                launchActivity(StaffDatabaseActivity.class);
            } else if (id == R.id.nav_db_timetable) {
                Intent intent = new Intent(this, TimetableActivity.class);
                intent.putExtra("ORG_ID", orgId);
                intent.putExtra("USER_ROLE", "admin");
                startActivity(intent);
            } else if (id == R.id.nav_analytics_registrations) {
                launchAnalyticsActivity(0);
            } else if (id == R.id.nav_analytics_biometrics) {
                launchAnalyticsActivity(1);
            } else if (id == R.id.nav_analytics_intruders) {
                launchAnalyticsActivity(2);
            } else if (id == R.id.nav_drawer_logout) {
                handleLogout();
            }
            return false;
        });

        
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_admin_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_admin_home) {
                    return true;
                } else if (itemId == R.id.nav_admin_live_exam) {
                    launchActivity(LiveExamPreCheckActivity.class);
                    return false;
                } else if (itemId == R.id.nav_admin_alerts) {
                    launchAnalyticsActivity(2); 
                    return false;
                }
                return false;
            });
        }

        
        if (binding.cvExamRegistry != null) {
            binding.cvExamRegistry.setOnClickListener(v -> {
                launchActivity(LiveExamPreCheckActivity.class);
            });
        }

        fetchDashboardData();
    }

    private void launchUploadActivity(String type) {
        Intent intent = new Intent(this, CsvUploadActivity.class);
        intent.putExtra("ORG_ID", orgId);
        intent.putExtra("UPLOAD_TYPE", type);
        startActivity(intent);
    }

    private void launchActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.putExtra("ORG_ID", orgId);
        startActivity(intent);
    }

    private void launchAnalyticsActivity(int tabIndex) {
        Intent intent = new Intent(this, AnalyticsDashboardActivity.class);
        intent.putExtra("ORG_ID", orgId);
        intent.putExtra("SELECTED_TAB", tabIndex);
        startActivity(intent);
    }

    private void handleLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> {
                getSharedPreferences("VisionSecureSession", MODE_PRIVATE).edit().clear().apply();
                stopService(new Intent(this, IntruderAlertService.class));
                Intent intent = new Intent(this, com.example.vs_project.ui.auth.GetStartedActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDashboardData();
    }

    private void fetchDashboardData() {
        if (orgId == null || orgId.isEmpty()) return;
        
        
        db.collection("students").whereEqualTo("org_id", orgId).get().addOnCompleteListener(task -> {
            try {
                if (task.isSuccessful() && task.getResult() != null) {
                    binding.tvStudentCount.setText(String.valueOf(task.getResult().size()));
                } else {
                    binding.tvStudentCount.setText("0");
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
        
        
        db.collection("users").whereEqualTo("org_id", orgId)
          .whereIn("role", Arrays.asList("faculty", "incharge", "invigilator"))
          .get().addOnCompleteListener(task -> {
            try {
                if (task.isSuccessful() && task.getResult() != null) {
                    binding.tvStaffCount.setText(String.valueOf(task.getResult().size()));
                } else {
                    binding.tvStaffCount.setText("0");
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
        
        
        db.collection("timetable").whereEqualTo("org_id", orgId).get().addOnCompleteListener(task -> {
            try {
                if (task.isSuccessful() && task.getResult() != null) {
                    binding.tvCameraCount.setText(String.valueOf(task.getResult().size()));
                } else {
                    binding.tvCameraCount.setText("0");
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        
        db.collection("students")
          .whereEqualTo("org_id", orgId)
          .whereEqualTo("exam_registered", true)
          .get().addOnCompleteListener(task -> {
            try {
                if (task.isSuccessful() && task.getResult() != null) {
                    int count = task.getResult().size();
                    binding.tvLiveRegistryStatus.setText(count + " Total Registered");
                } else {
                    binding.tvLiveRegistryStatus.setText("0 Total Registered");
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        
        db.collection("organizations").document(orgId).collection("security_logs")
          .whereEqualTo("type", "INTRUDER_ALERT")
          .get().addOnCompleteListener(task -> {
            try {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    int alerts = task.getResult().size();
                    if (binding.tvSecurityTitle != null) {
                        binding.tvSecurityTitle.setText("Alerts Detected!");
                        binding.tvSecurityTitle.setTextColor(android.graphics.Color.RED);
                    }
                } else {
                    if (binding.tvSecurityTitle != null) {
                        binding.tvSecurityTitle.setText("System Secure");
                        binding.tvSecurityTitle.setTextColor(androidx.core.content.ContextCompat.getColor(AdminDashboardActivity.this, R.color.textPrimary));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
