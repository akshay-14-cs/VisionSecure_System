package com.example.vs_project.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class StaffDatabaseActivity extends AppCompatActivity {

    private TableLayout tableStaff;
    private FirebaseFirestore db;
    private String orgId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_database);

        tableStaff = findViewById(R.id.tableStaff);
        db = FirebaseFirestore.getInstance();
        
        if (getIntent() != null && getIntent().hasExtra("ORG_ID")) {
            orgId = getIntent().getStringExtra("ORG_ID");
        } else {
            orgId = "org_college_main"; 
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Faculty & Staff Database");
        }

        loadStaffData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadStaffData() {
        db.collection("users")
            .whereEqualTo("org_id", orgId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                tableStaff.removeAllViews(); 
                int rowIndex = 0;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String role = doc.getString("role");
                    if ("faculty".equals(role) || "invigilator".equals(role) || "incharge".equals(role)) {
                        String name = doc.getString("name");
                        String department = doc.getString("department");
                        String docId = doc.getId();
                        
                        
                        String rawId = docId;
                        if (docId.startsWith(orgId + "_")) {
                            rawId = docId.substring(orgId.length() + 1);
                        }
                        
                        boolean isApproved = doc.getBoolean("is_approved") != null ? doc.getBoolean("is_approved") : false;
                        boolean isIncharge = doc.getBoolean("is_incharge") != null ? doc.getBoolean("is_incharge") : false;
                        
                        addStaffRow(rawId, name, department, isApproved, isIncharge, docId, rowIndex++);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load staff data", Toast.LENGTH_SHORT).show();
            });
    }

    private void addStaffRow(String staffId, String name, String department, boolean isApproved, boolean isIncharge, String docId, int rowIndex) {
        TableRow row = new TableRow(this);
        row.setPadding(4, 8, 4, 8);
        
        if (rowIndex % 2 == 0) {
            row.setBackgroundColor(Color.parseColor("#1E293B"));
        } else {
            row.setBackgroundColor(Color.parseColor("#0F172A"));
        }

        TextView tvId = createTextView(staffId, 80);
        TextView tvName = createTextView(name, 120);
        TextView tvDept = createTextView(department, 120);
        
        com.google.android.material.switchmaterial.SwitchMaterial switchIncharge = new com.google.android.material.switchmaterial.SwitchMaterial(this);
        switchIncharge.setChecked(isIncharge);
        switchIncharge.setText("Incharge");
        switchIncharge.setTextColor(Color.parseColor("#E2E8F0"));
        switchIncharge.setTextSize(12);
        
        switchIncharge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateInchargePrivilege(docId, isChecked);
        });

        Button btnAction = new Button(this);
        btnAction.setAllCaps(false);
        btnAction.setTextSize(12);
        
        if (isApproved) {
            btnAction.setText("Approved");
            btnAction.setBackgroundColor(Color.parseColor("#10B981")); 
            btnAction.setTextColor(Color.WHITE);
            btnAction.setEnabled(false);
        } else {
            btnAction.setText("Approve");
            btnAction.setBackgroundColor(Color.parseColor("#2563EB")); 
            btnAction.setTextColor(Color.WHITE);
            btnAction.setOnClickListener(v -> approveStaff(docId, btnAction));
        }

        TableRow.LayoutParams params = new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 4, 8, 4);
        switchIncharge.setLayoutParams(params);
        btnAction.setLayoutParams(params);

        row.addView(tvId);
        row.addView(tvName);
        row.addView(tvDept);
        row.addView(switchIncharge);
        row.addView(btnAction);

        tableStaff.addView(row);
    }

    private TextView createTextView(String text, int minWidthDp) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "N/A");
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(13);
        tv.setPadding(8, 16, 8, 16);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        
        float scale = getResources().getDisplayMetrics().density;
        tv.setMinWidth((int) (minWidthDp * scale + 0.5f));
        return tv;
    }

    private void updateInchargePrivilege(String docId, boolean isIncharge) {
        db.collection("users").document(docId).update("is_incharge", isIncharge)
            .addOnSuccessListener(aVoid -> {
                String status = isIncharge ? "granted" : "revoked";
                Toast.makeText(this, "Incharge privileges " + status, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update privileges", Toast.LENGTH_SHORT).show();
            });
    }

    private void approveStaff(String docId, Button btnAction) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("is_approved", true);
        updates.put("role", "faculty"); 
        
        db.collection("users").document(docId).update(updates)
            .addOnSuccessListener(aVoid -> {
                btnAction.setText("Approved");
                btnAction.setBackgroundColor(Color.parseColor("#10B981"));
                btnAction.setEnabled(false);
                Toast.makeText(this, "Staff approved successfully!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to approve: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
