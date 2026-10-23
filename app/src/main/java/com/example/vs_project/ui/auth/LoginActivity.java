package com.example.vs_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityLoginBinding;
import com.example.vs_project.ui.dashboard.AdminDashboardActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private String expectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        expectedRole = getIntent().getStringExtra("EXPECTED_ROLE");
        if (expectedRole == null) expectedRole = "student"; 
        
        if (binding.tvPortalName != null) {
            String portalName = expectedRole.substring(0, 1).toUpperCase() + expectedRole.substring(1).toLowerCase() + " Portal";
            binding.tvPortalName.setText(portalName);
            binding.tvPortalName.setTextColor(android.graphics.Color.parseColor("#3B82F6")); 
            binding.tvPortalName.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();

        
        binding.etUserId.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (binding.interactiveEye != null) binding.interactiveEye.lookDown();
            }
        });
        
        binding.etOrgId.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (binding.interactiveEye != null) binding.interactiveEye.lookNormal();
            }
        });

        binding.etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (binding.interactiveEye != null) binding.interactiveEye.closeEye();
            } else {
                if (binding.interactiveEye != null) binding.interactiveEye.lookNormal();
            }
        });

        binding.tvForgotPassword.setVisibility(View.VISIBLE);
        binding.tvForgotPassword.setOnClickListener(v -> {
            if ("admin".equalsIgnoreCase(expectedRole)) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Forgot Password")
                    .setMessage("Please contact your Organization Administrator to reset your password.")
                    .setPositiveButton("OK", null)
                    .show();
            }
        });

        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String orgIdInput = (binding.etOrgId != null) ? binding.etOrgId.getText().toString().trim().toUpperCase() : "";
                String rawUserId = binding.etUserId.getText().toString().trim().toUpperCase();
                
                
                String normalizedOrg = orgIdInput.replace("-", "_");
                String normalizedUser = rawUserId.replace("-", "_");
                if (!normalizedOrg.isEmpty() && normalizedUser.startsWith(normalizedOrg + "_")) {
                    if (rawUserId.length() > orgIdInput.length() + 1) {
                        rawUserId = rawUserId.substring(orgIdInput.length() + 1);
                    }
                }
                
                final String userId = "admin".equalsIgnoreCase(expectedRole) ? rawUserId.toLowerCase() : rawUserId;
                final String password = binding.etPassword.getText().toString().trim();

                if (userId.isEmpty() || orgIdInput.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter Org ID and User ID", Toast.LENGTH_SHORT).show();
                    return;
                }

                binding.btnLogin.setEnabled(false);
                binding.btnLogin.setText("Logging in...");

                com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                
                if ("student".equalsIgnoreCase(expectedRole)) {
                    
                    String prefixedId = orgIdInput + "_" + userId;
                    db.collection("students").document(prefixedId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            handleLoginDocument(db, task.getResult(), orgIdInput, password, userId, "student");
                        } else {
                            showLoginError("Student not found!");
                        }
                    });
                } else if ("admin".equalsIgnoreCase(expectedRole)) {
                    
                    db.collection("users").document(userId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String role = task.getResult().getString("role");
                            if ("admin".equalsIgnoreCase(role)) {
                                handleLoginDocument(db, task.getResult(), orgIdInput, password, userId, role);
                            } else {
                                showLoginError("User is not an Admin!");
                            }
                        } else {
                            showLoginError("Admin not found!");
                        }
                    });
                } else {
                    
                    String prefixedId = orgIdInput + "_" + userId;
                    db.collection("users").document(prefixedId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String role = task.getResult().getString("role");
                            boolean isIncharge = task.getResult().getBoolean("is_incharge") != null && task.getResult().getBoolean("is_incharge");

                            if ("incharge".equalsIgnoreCase(expectedRole)) {
                                if (isIncharge) {
                                    handleLoginDocument(db, task.getResult(), orgIdInput, password, userId, expectedRole);
                                } else {
                                    showLoginError("Faculty is not authorized as Incharge!");
                                }
                            } else if ("faculty".equalsIgnoreCase(expectedRole) || "invigilator".equalsIgnoreCase(expectedRole)) {
                                if ("faculty".equalsIgnoreCase(role) || "invigilator".equalsIgnoreCase(role)) {
                                    handleLoginDocument(db, task.getResult(), orgIdInput, password, userId, "invigilator");
                                } else {
                                    showLoginError("User is not authorized as Staff!");
                                }
                            } else {
                                showLoginError("User does not have the required role!");
                            }
                        } else {
                            if (!task.isSuccessful()) {
                                showLoginError("DB Error: " + task.getException().getMessage());
                            } else {
                                showLoginError("Staff not found: " + prefixedId);
                            }
                        }
                    });
                }
            }
        });
    }

    private void showLoginError(String message) {
        binding.btnLogin.setEnabled(true);
        binding.btnLogin.setText("Login Securely");
        if (binding.interactiveEye != null) binding.interactiveEye.lookLeftRight();
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleLoginDocument(com.google.firebase.firestore.FirebaseFirestore db, com.google.firebase.firestore.DocumentSnapshot doc, String orgId, String password, String userId, String role) {
        String dbPasswordHash = doc.getString("password_hash");
        if (dbPasswordHash == null || dbPasswordHash.isEmpty()) {
            
            Intent intent = new Intent(LoginActivity.this, FirstTimePasswordActivity.class);
            intent.putExtra("ORG_ID", orgId);
            intent.putExtra("USER_ID", doc.getId());
            intent.putExtra("USER_ROLE", role);
            intent.putExtra("USER_NAME", doc.getString("name"));
            intent.putExtra("ORG_TYPE", doc.getString("org_type"));
            startActivity(intent);
            finish();
            return;
        }

        if (dbPasswordHash.equals(hashPassword(password))) {
            String name = doc.getString("name");
            String orgType = doc.getString("org_type");
            logAudit(userId, role, orgId);
            startDashboard(role, orgId, name, orgType, doc.getId());
        } else {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setText("Login Securely");
            if (binding.interactiveEye != null) binding.interactiveEye.lookLeftRight();
            Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void startDashboard(String role, String orgId, String name, String orgType, String userId) {
        Class<?> targetActivity;
        if ("admin".equalsIgnoreCase(role)) {
            targetActivity = AdminDashboardActivity.class;
        } else if ("incharge".equalsIgnoreCase(role)) {
            targetActivity = com.example.vs_project.ui.dashboard.InchargeDashboardActivity.class;
        } else if ("student".equalsIgnoreCase(role)) {
            targetActivity = com.example.vs_project.ui.dashboard.DashboardActivity.class;
        } else if ("invigilator".equalsIgnoreCase(role)) {
            targetActivity = com.example.vs_project.ui.dashboard.InvigilatorDashboardActivity.class;
        } else {
            targetActivity = com.example.vs_project.ui.dashboard.FacultyDashboardActivity.class;
        }
        
        android.content.SharedPreferences prefs = getSharedPreferences("VisionSecureSession", MODE_PRIVATE);
        prefs.edit()
             .putBoolean("IS_LOGGED_IN", true)
             .putString("USER_ROLE", role)
             .putString("ORG_ID", orgId)
             .putString("USER_NAME", name)
             .putString("ORG_TYPE", orgType)
             .putString("USER_ID", userId)
             .apply();

        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("ORG_ID", orgId);
        intent.putExtra("USER_NAME", name);
        intent.putExtra("ORG_TYPE", orgType);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
    
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        } else {
            
        }
    }
    
    private void logAudit(String userId, String role, String orgId) {
        try {
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    double lat = 0.0;
                    double lng = 0.0;
                    if (location != null) {
                        lat = location.getLatitude();
                        lng = location.getLongitude();
                    }
                    
                    com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                    Map<String, Object> audit = new HashMap<>();
                    audit.put("user_id", userId);
                    audit.put("role", role);
                    audit.put("orgId", orgId);
                    audit.put("gps_lat", lat);
                    audit.put("gps_lng", lng);
                    audit.put("timestamp", System.currentTimeMillis());
                    
                    db.collection("login_audits").add(audit);
                });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
