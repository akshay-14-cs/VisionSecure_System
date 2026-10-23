package com.example.vs_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;
import android.view.View;
import android.widget.LinearLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etUserId, etSecurityAnswer1, etSecurityAnswer2, etNewPassword;
    private TextInputLayout tilSecurityAnswer1, tilSecurityAnswer2;
    private Button btnSubmitRequest, btnVerifyAnswers, btnResetPassword;
    private TextView tvBackToLogin;
    private LinearLayout llSecurityQuestions, llNewPassword;
    private FirebaseFirestore db;
    private DocumentSnapshot currentUserDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etUserId = findViewById(R.id.etUserId);
        etSecurityAnswer1 = findViewById(R.id.etSecurityAnswer1);
        etSecurityAnswer2 = findViewById(R.id.etSecurityAnswer2);
        tilSecurityAnswer1 = findViewById(R.id.tilSecurityAnswer1);
        tilSecurityAnswer2 = findViewById(R.id.tilSecurityAnswer2);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        btnVerifyAnswers = findViewById(R.id.btnVerifyAnswers);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        llSecurityQuestions = findViewById(R.id.llSecurityQuestions);
        llNewPassword = findViewById(R.id.llNewPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        db = FirebaseFirestore.getInstance();

        btnSubmitRequest.setOnClickListener(v -> {
            String userId = etUserId.getText().toString().trim();
            if (userId.isEmpty()) {
                Toast.makeText(this, "Please enter your User ID", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmitRequest.setEnabled(false);
            btnSubmitRequest.setText("Submitting...");

            
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentUserDoc = documentSnapshot;
                    Boolean isLocked = documentSnapshot.getBoolean("is_locked");
                    if (Boolean.TRUE.equals(isLocked)) {
                        Toast.makeText(this, "ACCOUNT LOCKED due to too many failed attempts. Contact Super Admin.", Toast.LENGTH_LONG).show();
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText("Submit Reset Request");
                        return;
                    }

                    String role = documentSnapshot.getString("role");
                    String orgId = documentSnapshot.getString("org_id");

                    if (!"admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Forgot Password facility is ONLY available for Admins. Please contact your Administrator.", Toast.LENGTH_LONG).show();
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText("Submit Reset Request");
                        return;
                    }

                    
                    String q1Text = documentSnapshot.getString("sec_q1_text");
                    String q2Text = documentSnapshot.getString("sec_q2_text");

                    if (q1Text == null || q2Text == null) {
                        Toast.makeText(this, "Security questions not configured for this account. Contact Support.", Toast.LENGTH_LONG).show();
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText("Submit Reset Request");
                        return;
                    }

                    tilSecurityAnswer1.setHint("Q1: " + q1Text);
                    tilSecurityAnswer2.setHint("Q2: " + q2Text);

                    etUserId.setEnabled(false);
                    btnSubmitRequest.setVisibility(View.GONE);
                    llSecurityQuestions.setVisibility(View.VISIBLE);
                } else {
                    btnSubmitRequest.setEnabled(true);
                    btnSubmitRequest.setText("Submit Reset Request");
                    Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                btnSubmitRequest.setEnabled(true);
                btnSubmitRequest.setText("Submit Reset Request");
                Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
            });
        });

        btnVerifyAnswers.setOnClickListener(v -> {
            if (currentUserDoc == null) return;
            String ans1 = etSecurityAnswer1.getText().toString().trim().toLowerCase();
            String ans2 = etSecurityAnswer2.getText().toString().trim().toLowerCase();
            
            if (ans1.isEmpty() || ans2.isEmpty()) {
                Toast.makeText(this, "Please answer both questions", Toast.LENGTH_SHORT).show();
                return;
            }

            String hashedAns1 = hashPassword(ans1);
            String hashedAns2 = hashPassword(ans2);

            String storedAns1 = currentUserDoc.getString("sec_q1_hash");
            String storedAns2 = currentUserDoc.getString("sec_q2_hash");

            if (hashedAns1.equals(storedAns1) && hashedAns2.equals(storedAns2)) {
                
                db.collection("users").document(currentUserDoc.getId()).update("failed_reset_attempts", 0);
                llSecurityQuestions.setVisibility(View.GONE);
                llNewPassword.setVisibility(View.VISIBLE);
            } else {
                
                Long failedAttemptsLong = currentUserDoc.getLong("failed_reset_attempts");
                int failedAttempts = (failedAttemptsLong != null) ? failedAttemptsLong.intValue() : 0;
                failedAttempts++;

                if (failedAttempts >= 3) {
                    db.collection("users").document(currentUserDoc.getId()).update(
                        "failed_reset_attempts", failedAttempts,
                        "is_locked", true
                    ).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "ACCOUNT LOCKED! 3 failed attempts.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, GetStartedActivity.class));
                        finish();
                    });
                } else {
                    db.collection("users").document(currentUserDoc.getId()).update("failed_reset_attempts", failedAttempts);
                    int remaining = 3 - failedAttempts;
                    Toast.makeText(this, "Incorrect! " + remaining + " attempts remaining.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnResetPassword.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString().trim();
            if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,14}$")) {
                etNewPassword.setError("Must be 8-14 chars, with Uppercase, Lowercase, Number & Symbol");
                return;
            }

            btnResetPassword.setEnabled(false);
            btnResetPassword.setText("Updating...");

            String hashedNewPassword = hashPassword(newPassword);
            
            db.collection("users").document(currentUserDoc.getId())
                .update("password_hash", hashedNewPassword)
                .addOnSuccessListener(aVoid -> {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Password Reset Successful")
                        .setMessage("Your password has been successfully updated. You can now login with your new credentials.")
                        .setPositiveButton("Go to Login", (dialog, which) -> {
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        })
                        .setCancelable(false)
                        .show();
                })
                .addOnFailureListener(e -> {
                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText("Reset Password");
                    Toast.makeText(this, "Failed to reset password.", Toast.LENGTH_SHORT).show();
                });
        });

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, GetStartedActivity.class));
            finish();
        });
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
}
