package com.example.vs_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityFirstTimePasswordBinding;
import com.example.vs_project.ui.dashboard.AdminDashboardActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.text.Editable;
import android.text.TextWatcher;

public class FirstTimePasswordActivity extends AppCompatActivity {

    private ActivityFirstTimePasswordBinding binding;
    private String userId;
    private String userRole;
    private String orgId;
    private String userName;
    private String orgType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFirstTimePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            userId = getIntent().getStringExtra("USER_ID");
            userRole = getIntent().getStringExtra("USER_ROLE");
            orgId = getIntent().getStringExtra("ORG_ID");
            userName = getIntent().getStringExtra("USER_NAME");
            orgType = getIntent().getStringExtra("ORG_TYPE");
        }

        binding.etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,14}$")) {
                    binding.tilNewPassword.setError("Must be 8 to 14 characters long, including uppercase, lowercase, number, and a symbol");
                } else {
                    binding.tilNewPassword.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnSetPassword.setOnClickListener(v -> {
            String newPassword = binding.etNewPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,14}$")) {
                binding.tilNewPassword.setError("Must be 8 to 14 characters long, including uppercase, lowercase, number, and a symbol");
                Toast.makeText(this, "Fix errors in red before submitting", Toast.LENGTH_LONG).show();
                return;
            }

            binding.btnSetPassword.setEnabled(false);
            binding.btnSetPassword.setText("Updating...");

            updatePasswordInFirestore(newPassword);
        });
    }

    private void updatePasswordInFirestore(String newPassword) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String hashedPassword = hashPassword(newPassword);
        
        String collection = "student".equalsIgnoreCase(userRole) ? "students" : "users";

        db.collection(collection).document(userId)
                .update(
                    "password_hash", hashedPassword,
                    "is_first_login", false
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    
                    Class<?> targetActivity;
                    if ("admin".equalsIgnoreCase(userRole)) {
                        targetActivity = AdminDashboardActivity.class;
                    } else if ("incharge".equalsIgnoreCase(userRole)) {
                        targetActivity = com.example.vs_project.ui.dashboard.InchargeDashboardActivity.class;
                    } else if ("student".equalsIgnoreCase(userRole)) {
                        targetActivity = com.example.vs_project.ui.dashboard.DashboardActivity.class;
                    } else if ("invigilator".equalsIgnoreCase(userRole)) {
                        targetActivity = com.example.vs_project.ui.dashboard.InvigilatorDashboardActivity.class;
                    } else {
                        targetActivity = com.example.vs_project.ui.dashboard.FacultyDashboardActivity.class;
                    }

                    android.content.SharedPreferences prefs = getSharedPreferences("VisionSecureSession", MODE_PRIVATE);
                    prefs.edit()
                         .putBoolean("IS_LOGGED_IN", true)
                         .putString("USER_ROLE", userRole)
                         .putString("ORG_ID", orgId)
                         .putString("USER_NAME", userName)
                         .putString("ORG_TYPE", orgType)
                         .putString("USER_ID", userId)
                         .apply();

                    Intent intent = new Intent(FirstTimePasswordActivity.this, targetActivity);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("USER_ROLE", userRole);
                    intent.putExtra("ORG_ID", orgId);
                    intent.putExtra("USER_NAME", userName);
                    intent.putExtra("ORG_TYPE", orgType);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSetPassword.setEnabled(true);
                    binding.btnSetPassword.setText("Update Password");
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
