package com.example.vs_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityAddSecurityQuestionsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AddSecurityQuestionsActivity extends AppCompatActivity {

    private ActivityAddSecurityQuestionsBinding binding;
    private FirebaseFirestore db;
    private String orgId;
    private String adminUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddSecurityQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        orgId = getIntent().getStringExtra("ORG_ID");
        adminUserId = getIntent().getStringExtra("ADMIN_USER_ID");

        if (orgId == null || adminUserId == null) {
            Toast.makeText(this, "Session invalid. Please try registering again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.btnSaveSecurity.setOnClickListener(v -> {
            String q1 = (binding.etSecurityQuestion1.getText() != null) ? binding.etSecurityQuestion1.getText().toString().trim() : "";
            String ans1 = (binding.etSecurityAnswer1.getText() != null) ? binding.etSecurityAnswer1.getText().toString().trim().toLowerCase() : "";
            String q2 = (binding.etSecurityQuestion2.getText() != null) ? binding.etSecurityQuestion2.getText().toString().trim() : "";
            String ans2 = (binding.etSecurityAnswer2.getText() != null) ? binding.etSecurityAnswer2.getText().toString().trim().toLowerCase() : "";

            if (q1.isEmpty() || ans1.isEmpty() || q2.isEmpty() || ans2.isEmpty()) {
                Toast.makeText(this, "Please provide two custom security questions and answers.", Toast.LENGTH_LONG).show();
                return;
            }

            binding.btnSaveSecurity.setEnabled(false);
            binding.btnSaveSecurity.setText("Saving...");

            db.collection("users").document(adminUserId).update(
                    "sec_q1_text", q1,
                    "sec_q1_hash", hashPassword(ans1),
                    "sec_q2_text", q2,
                    "sec_q2_hash", hashPassword(ans2)
            ).addOnSuccessListener(aVoid -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Registration Successful!")
                        .setMessage("Please save these details:\n\nOrg ID: " + orgId + "\nAdmin User ID: " + adminUserId + "\n\nYou will need these to login.")
                        .setPositiveButton("Go to Login", (dialog, which) -> {
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.putExtra("EXPECTED_ROLE", "admin");
                            startActivity(intent);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }).addOnFailureListener(e -> {
                binding.btnSaveSecurity.setEnabled(true);
                binding.btnSaveSecurity.setText("SAVE AND COMPLETE");
                Toast.makeText(this, "Failed to save security questions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
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
