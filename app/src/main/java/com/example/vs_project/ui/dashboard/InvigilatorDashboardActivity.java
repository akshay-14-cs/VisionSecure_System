package com.example.vs_project.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityInvigilatorDashboardBinding;
import com.example.vs_project.ui.scanner.FaceScannerActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.content.FileProvider;
import android.net.Uri;

public class InvigilatorDashboardActivity extends AppCompatActivity {

    private ActivityInvigilatorDashboardBinding binding;
    private String orgId = "";
    private String userName = "";
    private String userId = "";
    private FirebaseFirestore db;

    private List<QueryDocumentSnapshot> timetableDocs = new ArrayList<>();
    private List<String> dates = new ArrayList<>();
    private List<String> timings = new ArrayList<>();
    private List<String> rooms = new ArrayList<>();
    
    private String selectedDate = "";
    private String selectedTiming = "";
    private String selectedRoom = "";
    private String selectedSubject = "";

    private LogAdapter liveAdapter;
    private List<LogItem> liveLogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvigilatorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        if (getIntent() != null) {
            orgId = getIntent().getStringExtra("ORG_ID") != null ? getIntent().getStringExtra("ORG_ID") : "";
            userName = getIntent().getStringExtra("USER_NAME");
            userId = getIntent().getStringExtra("USER_ID") != null ? getIntent().getStringExtra("USER_ID") : "";
        }

        if (userName == null || userName.equals("null") || userName.trim().isEmpty()) {
            userName = "Invigilator";
        }

        binding.tvWelcomeInvigilator.setText("Welcome, " + userName);
        com.example.vs_project.utils.ThemeUtils.setupSwitch(this, binding.switchTheme);

        liveAdapter = new LogAdapter(liveLogs, false, null);
        binding.rvLiveVerifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLiveVerifications.setAdapter(liveAdapter);

        fetchTimetable();

        binding.spinnerDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDate = dates.get(position);
                updateTimingsSpinner();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerTiming.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position >= 0 && position < timings.size()) {
                    selectedTiming = timings.get(position);
                    updateRoomsSpinner();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position >= 0 && position < rooms.size()) {
                    selectedRoom = rooms.get(position);
                    updateExamDetails();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.cardVerifyFace.setOnClickListener(v -> {
            if (selectedDate.isEmpty() || selectedTiming.isEmpty() || selectedRoom.isEmpty()) {
                Toast.makeText(this, "Please select Date, Timing and Room", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(InvigilatorDashboardActivity.this, FaceScannerActivity.class);
            intent.putExtra("SCANNER_MODE", "VERIFICATION");
            intent.putExtra("ORG_ID", orgId);
            intent.putExtra("SELECTED_DATE", selectedDate);
            intent.putExtra("SELECTED_TIMING", selectedTiming);
            intent.putExtra("SELECTED_ROOM", selectedRoom);
            intent.putExtra("SELECTED_SUBJECT", selectedSubject);
            startActivity(intent);
        });

        if (binding.cardViewTimetable != null) {
            binding.cardViewTimetable.setOnClickListener(v -> {
                Intent intent = new Intent(InvigilatorDashboardActivity.this, TimetableActivity.class);
                intent.putExtra("ORG_ID", orgId);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("USER_ROLE", "invigilator");
                startActivity(intent);
            });
        }
        
        if (binding.cardDownloadPdf != null) {
            binding.cardDownloadPdf.setOnClickListener(v -> generateVerificationPdf());
        }

        binding.btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    
                    android.content.SharedPreferences prefs = getSharedPreferences("VisionSecureSession", MODE_PRIVATE);
                    prefs.edit().clear().apply();
                    
                    Intent intent = new Intent(InvigilatorDashboardActivity.this, com.example.vs_project.ui.auth.GetStartedActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void fetchTimetable() {
        if(orgId == null || orgId.isEmpty()) return;
        
        db.collection("timetable").get().addOnCompleteListener(task -> {
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerDate.setAdapter(adapter);
                if(!dates.isEmpty()) {
                    selectedDate = dates.get(0);
                    updateTimingsSpinner();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rooms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRoom.setAdapter(adapter);
        if(!rooms.isEmpty()) {
            selectedRoom = rooms.get(0);
            updateExamDetails();
        } else {
            selectedRoom = "";
            binding.tvExamDetails.setText("No exams found for this selection.");
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
                binding.tvExamDetails.setText(details);
                fetchLiveVerifications();
                return;
            }
        }
        binding.tvExamDetails.setText("");
        liveLogs.clear();
        if (liveAdapter != null) liveAdapter.notifyDataSetChanged();
    }
    
    private void fetchLiveVerifications() {
        if (orgId.isEmpty() || userId.isEmpty() || selectedDate.isEmpty() || selectedTiming.isEmpty()) return;

        db.collection("attendance")
            .whereEqualTo("org_id", orgId)
            .whereEqualTo("supervisor_id", userId)
            .whereEqualTo("exam_date", selectedDate)
            .whereEqualTo("exam_time", selectedTiming)
            .addSnapshotListener((value, error) -> {
                if (error != null || value == null) return;
                
                liveLogs.clear();
                for (QueryDocumentSnapshot doc : value) {
                    String sId = doc.getString("student_id");
                    String ts = doc.getString("verification_timestamp");
                    if (ts != null && ts.length() > 16) ts = ts.substring(0, 16).replace("T", " ");
                    liveLogs.add(new LogItem("Verified", "Student ID: " + sId, ts, null));
                }
                if (liveAdapter != null) liveAdapter.notifyDataSetChanged();
            });
    }

    private void generateVerificationPdf() {
        if (orgId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "User details missing", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate.isEmpty() || selectedTiming.isEmpty()) {
            Toast.makeText(this, "Please select a Date and Timing to filter the report", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Fetching logs...", Toast.LENGTH_SHORT).show();

        db.collection("attendance")
            .whereEqualTo("org_id", orgId)
            .whereEqualTo("supervisor_id", userId)
            .whereEqualTo("exam_date", selectedDate)
            .whereEqualTo("exam_time", selectedTiming)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        docs.add(doc);
                    }
                    createPdfDocument(docs);
                } else {
                    Toast.makeText(this, "Failed to fetch logs", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void createPdfDocument(List<QueryDocumentSnapshot> logs) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); 
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        
        paint.setTextSize(24f);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        canvas.drawText("Face Verification Report", 180, 50, paint);

        paint.setTextSize(14f);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        canvas.drawText("Invigilator: " + userName, 50, 90, paint);
        canvas.drawText("Date: " + selectedDate, 50, 110, paint);
        canvas.drawText("Timing: " + selectedTiming, 50, 130, paint);
        
        if (logs.isEmpty()) {
            canvas.drawText("No verified students found for this time slot.", 50, 170, paint);
        } else {
            
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
            canvas.drawText("Student ID", 50, 180, paint);
            canvas.drawText("Subject", 150, 180, paint);
            canvas.drawText("Hall", 250, 180, paint);
            canvas.drawText("Status", 350, 180, paint);
            canvas.drawText("Timestamp", 450, 180, paint);
            
            canvas.drawLine(50, 185, 550, 185, paint);

            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            int y = 205;
            for (QueryDocumentSnapshot doc : logs) {
                String sId = doc.getString("student_id");
                String sub = doc.getString("subject_code");
                String hall = doc.getString("room");
                String status = doc.getString("status");
                String ts = doc.getString("verification_timestamp");
                if (ts != null && ts.length() > 16) {
                    ts = ts.substring(0, 16).replace("T", " "); 
                }
                
                canvas.drawText(sId != null ? sId : "", 50, y, paint);
                canvas.drawText(sub != null ? sub : "", 150, y, paint);
                canvas.drawText(hall != null ? hall : "", 250, y, paint);
                canvas.drawText(status != null ? status : "", 350, y, paint);
                canvas.drawText(ts != null ? ts : "", 450, y, paint);
                
                y += 20;
                
                if (y > 800) {
                    
                    break;
                }
            }
        }
        
        document.finishPage(page);

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, "VerificationLog_" + selectedDate + "_" + System.currentTimeMillis() + ".pdf");
            document.writeTo(new FileOutputStream(file));
            
            
            Uri pdfUri;
            try {
                pdfUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
            } catch (Exception e) {
                
                pdfUri = Uri.fromFile(file);
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }
}
