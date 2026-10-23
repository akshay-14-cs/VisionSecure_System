package com.example.vs_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityOrgRegistrationBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import android.location.Location;
import android.view.inputmethod.EditorInfo;
import com.google.android.material.chip.Chip;
import android.graphics.Color;
import android.text.TextWatcher;
import android.text.Editable;

public class OrgRegistrationActivity extends AppCompatActivity {

    private ActivityOrgRegistrationBinding binding;
    private String orgType; 
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 28.7041; 
    private double currentLng = 77.1025; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrgRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        binding.btnFetchLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            } else {
                fetchLocation();
            }
        });

        binding.etDepartmentNames.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                String deptName = binding.etDepartmentNames.getText().toString().trim();
                if (!deptName.isEmpty()) {
                    addDepartmentChip(deptName);
                    binding.etDepartmentNames.setText("");
                }
                return true;
            }
            return false;
        });
        
        binding.etAdminMobile.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 10) {
                    binding.tilAdminMobile.setError("Mobile number must be exactly 10 digits");
                } else {
                    binding.tilAdminMobile.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etAdminPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,14}$")) {
                    binding.tilAdminPassword.setError("Must be 8-14 chars, with Uppercase, Lowercase, Number & Symbol");
                } else {
                    binding.tilAdminPassword.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        orgType = getIntent().getStringExtra("ORG_TYPE");
        if (orgType == null) orgType = "College";
        
        binding.tvHeader.setText("Register " + orgType);

        binding.tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(OrgRegistrationActivity.this, GetStartedActivity.class);
            startActivity(intent);
            finish();
        });

        binding.btnRegister.setOnClickListener(v -> {
            String orgName = binding.etOrgName.getText().toString().trim();
            String address = binding.etOrgAddress.getText().toString().trim();
            String adminName = binding.etAdminName.getText().toString().trim();
            String adminMobile = binding.etAdminMobile.getText().toString().trim();
            String password = binding.etAdminPassword.getText().toString().trim();

            int numDepartments = binding.cgDepartments.getChildCount();
            
            if (orgName.isEmpty() || address.isEmpty() || numDepartments == 0 || adminName.isEmpty() || adminMobile.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields and add at least one department", Toast.LENGTH_SHORT).show();
                return;
            }

            if (adminMobile.length() != 10) {
                binding.tilAdminMobile.setError("Admin mobile number must be exactly 10 digits");
                Toast.makeText(this, "Fix errors in red before registering", Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,14}$")) {
                binding.tilAdminPassword.setError("Must be 8-14 chars, with Uppercase, Lowercase, Number & Symbol");
                Toast.makeText(this, "Fix errors in red before registering", Toast.LENGTH_LONG).show();
                return;
            }

            
            List<String> deptList = new ArrayList<>();
            for (int i = 0; i < binding.cgDepartments.getChildCount(); i++) {
                Chip chip = (Chip) binding.cgDepartments.getChildAt(i);
                deptList.add(chip.getText().toString());
            }
            String deptNames = String.join(", ", deptList);

            binding.btnRegister.setEnabled(false);
            binding.btnRegister.setText("Registering...");

            
            String orgIdPrefix = orgType.toUpperCase();
            String uniqueId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String orgId = orgIdPrefix + "-" + uniqueId;
            String adminUserId = orgType.toLowerCase() + "_admin_" + uniqueId.toLowerCase();

            
            Map<String, Object> orgData = new HashMap<>();
            orgData.put("name", orgName);
            orgData.put("type", orgType);
            orgData.put("org_type", orgType);
            orgData.put("address", address);
            orgData.put("num_departments", numDepartments);
            orgData.put("department_names", deptNames);
            
            Map<String, Object> rules = new HashMap<>();
            rules.put("wifi_ssid", "CampusNet");
            rules.put("gps_lat", currentLat);
            rules.put("gps_lng", currentLng);
            rules.put("gps_long", currentLng);
            rules.put("geofence_radius_meters", 200);
            orgData.put("verification_rules", rules);
            
            
            orgData.put("wifi_ssid", "CampusNet");
            orgData.put("gps_lat", currentLat);
            orgData.put("gps_lng", currentLng);

            db.collection("organizations").document(orgId).set(orgData)
                .addOnSuccessListener(aVoid -> {
                    
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("org_id", orgId);
                    userData.put("name", adminName);
                    userData.put("mobile", adminMobile);
                    userData.put("role", "admin");
                    userData.put("password_hash", hashPassword(password));
                    userData.put("is_first_login", true);
                    userData.put("sec_q1_text", "");
                    userData.put("sec_q1_hash", "");
                    userData.put("sec_q2_text", "");
                    userData.put("sec_q2_hash", "");
                    userData.put("failed_reset_attempts", 0);
                    userData.put("is_locked", false);

                    db.collection("users").document(adminUserId).set(userData)
                        .addOnSuccessListener(aVoid2 -> {
                            
                            Intent intent = new Intent(OrgRegistrationActivity.this, AddSecurityQuestionsActivity.class);
                            intent.putExtra("ORG_ID", orgId);
                            intent.putExtra("ADMIN_USER_ID", adminUserId);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            binding.btnRegister.setEnabled(true);
                            binding.btnRegister.setText("Register Workspace");
                            Toast.makeText(this, "Failed to create admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                })
                .addOnFailureListener(e -> {
                    binding.btnRegister.setEnabled(true);
                    binding.btnRegister.setText("Register Workspace");
                    Toast.makeText(this, "Failed to create organization: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
    }

    private void fetchLocation() {
        binding.tvLocationStatus.setText("Fetching...");
        binding.tvLocationStatus.setTextColor(Color.parseColor("#FFA500")); 
        try {
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        binding.tvLocationStatus.setText("Location Acquired");
                        binding.tvLocationStatus.setTextColor(Color.parseColor("#4CAF50")); 
                    } else {
                        binding.tvLocationStatus.setText("Failed (Turn on GPS in Emulator Settings)");
                        binding.tvLocationStatus.setTextColor(Color.RED);
                    }
                });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void addDepartmentChip(String deptName) {
        Chip chip = new Chip(this);
        chip.setText(deptName);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            binding.cgDepartments.removeView(chip);
            updateDepartmentCount();
        });
        binding.cgDepartments.addView(chip);
        updateDepartmentCount();
    }

    private void updateDepartmentCount() {
        binding.etNumDepartments.setText(String.valueOf(binding.cgDepartments.getChildCount()));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
