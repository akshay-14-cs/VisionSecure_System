package com.example.vs_project.ui.dashboard;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.databinding.ActivityCsvUploadBinding;
import com.example.vs_project.network.ApiService;
import com.example.vs_project.network.RetrofitClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CsvUploadActivity extends AppCompatActivity {

    private ActivityCsvUploadBinding binding;
    private String orgId = "";
    private String uploadType = "TIMETABLE";
    private Uri selectedCsvUri = null;

    private final ActivityResultLauncher<String> csvPickerLauncher = 
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedCsvUri = uri;
                binding.tvSelectedFile.setText("File Selected: " + uri.getLastPathSegment());
                binding.btnUploadFile.setEnabled(true);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCsvUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            if (getIntent().hasExtra("ORG_ID")) orgId = getIntent().getStringExtra("ORG_ID");
            if (getIntent().hasExtra("UPLOAD_TYPE")) uploadType = getIntent().getStringExtra("UPLOAD_TYPE");
        }

        binding.tvHeader.setText("Upload " + uploadType + " (CSV)");
        binding.tvSelectedFile.setText("Upload Type: " + uploadType + "\nNo file selected");

        binding.btnSelectFile.setOnClickListener(v -> {
            csvPickerLauncher.launch("text/*"); 
        });

        binding.btnUploadFile.setOnClickListener(v -> {
            if (selectedCsvUri != null) {
                uploadCsvToServer();
            }
        });
    }

    private void uploadCsvToServer() {
        binding.btnUploadFile.setEnabled(false);
        binding.btnUploadFile.setText("Processing Locally...");
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                
                InputStream inputStream = getContentResolver().openInputStream(selectedCsvUri);
                File tempFile = new File(getCacheDir(), uploadType.toLowerCase() + "_upload.csv");
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.close();
                inputStream.close();

                
                BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                String line;
                boolean isFirstLine = true;
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                
                if ("STUDENTS".equals(uploadType)) {
                    List<Map<String, Object>> csvStudents = new ArrayList<>();
                    List<String> csvStudentIds = new ArrayList<>();
                    int skipped = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) { isFirstLine = false; continue; }
                        List<String> columns = parseCsvLine(line);
                        if (columns.size() < 2) { skipped++; continue; }
                        
                        String id = columns.get(0).trim();
                        String name = columns.size() > 1 ? columns.get(1).trim() : "";
                        String dept = columns.size() > 2 ? columns.get(2).trim() : "";
                        String courses = columns.size() > 3 ? columns.get(3).trim() : "";
                        String backlogCourses = columns.size() > 4 ? columns.get(4).trim() : "";
                        int backlogsCount = 0;
                        if (!backlogCourses.isEmpty() && !backlogCourses.equals("0")) {
                            backlogsCount = backlogCourses.split(",").length;
                        } else if (backlogCourses.equals("0")) {
                            backlogCourses = "";
                        }

                        if (id.isEmpty() || name.isEmpty() || dept.isEmpty() || courses.isEmpty()) {
                            skipped++; continue;
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("org_id", orgId);
                        data.put("student_id", id);
                        data.put("name", name);
                        data.put("department", dept);
                        data.put("courses", courses);
                        data.put("backlog_courses", backlogCourses);
                        data.put("backlogs", String.valueOf(backlogsCount));
                        
                        csvStudents.add(data);
                        csvStudentIds.add(id);
                    }
                    reader.close();
                    
                    final int finalSkipped = skipped;
                    final int finalProcessed = csvStudents.size();
                    
                    db.collection("students").whereEqualTo("org_id", orgId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            
                            
                            
                            
                            for (Map<String, Object> student : csvStudents) {
                                db.collection("students").document(orgId + "_" + student.get("student_id"))
                                  .set(student, SetOptions.merge());
                            }
                            
                            String msg = finalProcessed + " records processed and synced.";
                            if (finalSkipped > 0) msg += " (" + finalSkipped + " skipped)";
                            final String finalMessage = msg;
                            
                            runOnUiThread(() -> {
                                binding.progressBar.setVisibility(android.view.View.GONE);
                                Toast.makeText(CsvUploadActivity.this, finalMessage, Toast.LENGTH_LONG).show();
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                binding.progressBar.setVisibility(android.view.View.GONE);
                                Toast.makeText(CsvUploadActivity.this, "Failed to sync with DB", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                    });
                    
                } else if ("STAFF".equals(uploadType)) {
                    int count = 0;
                    int skipped = 0;
                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) { isFirstLine = false; continue; }
                        List<String> columns = parseCsvLine(line);
                        if (columns.size() < 2) { skipped++; continue; }
                        
                        String id = columns.get(0).trim();
                        String name = columns.size() > 1 ? columns.get(1).trim() : "";
                        String department = columns.size() > 2 ? columns.get(2).trim() : "";

                        if (id.isEmpty() || name.isEmpty()) { skipped++; continue; }

                        Map<String, Object> data = new HashMap<>();
                        data.put("org_id", orgId);
                        data.put("name", name);
                        data.put("department", department);
                        data.put("role", "faculty"); 
                        data.put("is_approved", false);
                        db.collection("users").document(orgId + "_" + id).set(data, com.google.firebase.firestore.SetOptions.merge());
                        count++;
                    }
                    reader.close();
                    String msg = count + " records processed.";
                    if (skipped > 0) msg += " (" + skipped + " rows skipped)";
                    final String finalMessage = msg;
                    
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(CsvUploadActivity.this, finalMessage, Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else if ("TIMETABLE".equals(uploadType)) {
                    List<Map<String, Object>> csvTimetable = new ArrayList<>();
                    int skipped = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) { isFirstLine = false; continue; }
                        List<String> columns = parseCsvLine(line);
                        if (columns.size() < 6) { skipped++; continue; }
                        
                        String date = columns.get(0).trim();
                        String timing = columns.get(1).trim();
                        String subjectCode = columns.get(2).trim();
                        String hallNo = columns.get(3).trim();
                        String facultyId = columns.get(4).trim();
                        String examMode = columns.get(5).trim(); 

                        if (date.isEmpty() || timing.isEmpty() || subjectCode.isEmpty() || hallNo.isEmpty() || facultyId.isEmpty() || examMode.isEmpty()) { 
                            skipped++; continue; 
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("org_id", orgId);
                        data.put("date", date);
                        data.put("timing", timing);
                        data.put("subject_code", subjectCode);
                        data.put("hall_no", hallNo);
                        data.put("faculty_id", facultyId);
                        data.put("exam_mode", examMode.toLowerCase());
                        
                        csvTimetable.add(data);
                    }
                    reader.close();
                    
                    
                    db.collection("timetable").whereEqualTo("org_id", orgId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<Map<String, Object>> existingTimetable = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                existingTimetable.add(doc.getData());
                            }
                            
                            
                            for (Map<String, Object> newRow : csvTimetable) {
                                String nDate = (String) newRow.get("date");
                                String nTime = (String) newRow.get("timing");
                                String nFac = (String) newRow.get("faculty_id");
                                String nHall = (String) newRow.get("hall_no");
                                
                                
                                for (Map<String, Object> exRow : existingTimetable) {
                                    if (nDate.equals(exRow.get("date")) && nTime.equals(exRow.get("timing")) && nFac.equals(exRow.get("faculty_id"))) {
                                        if (!nHall.equals(exRow.get("hall_no"))) {
                                            runOnUiThread(() -> {
                                                binding.progressBar.setVisibility(android.view.View.GONE);
                                                Toast.makeText(CsvUploadActivity.this, "CONFLICT! Faculty " + nFac + " assigned to multiple halls at " + nDate + " " + nTime, Toast.LENGTH_LONG).show();
                                                binding.btnUploadFile.setEnabled(true);
                                                binding.btnUploadFile.setText("Upload CSV");
                                            });
                                            return;
                                        }
                                    }
                                }
                                
                                
                                for (Map<String, Object> otherRow : csvTimetable) {
                                    if (newRow == otherRow) continue;
                                    if (nDate.equals(otherRow.get("date")) && nTime.equals(otherRow.get("timing")) && nFac.equals(otherRow.get("faculty_id"))) {
                                        if (!nHall.equals(otherRow.get("hall_no"))) {
                                            runOnUiThread(() -> {
                                                binding.progressBar.setVisibility(android.view.View.GONE);
                                                Toast.makeText(CsvUploadActivity.this, "CSV CONFLICT! Faculty " + nFac + " double-booked in CSV at " + nDate + " " + nTime, Toast.LENGTH_LONG).show();
                                                binding.btnUploadFile.setEnabled(true);
                                                binding.btnUploadFile.setText("Upload CSV");
                                            });
                                            return;
                                        }
                                    }
                                }
                            }
                            
                            
                            for (Map<String, Object> newRow : csvTimetable) {
                                String docId = orgId + "_" + newRow.get("date") + "_" + newRow.get("timing") + "_" + newRow.get("hall_no");
                                db.collection("timetable").document(docId).set(newRow, SetOptions.merge());
                            }
                            
                            runOnUiThread(() -> {
                                binding.progressBar.setVisibility(android.view.View.GONE);
                                Toast.makeText(CsvUploadActivity.this, csvTimetable.size() + " timetable entries synced.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                binding.progressBar.setVisibility(android.view.View.GONE);
                                Toast.makeText(CsvUploadActivity.this, "Failed to fetch existing timetable for conflict check.", Toast.LENGTH_SHORT).show();
                                binding.btnUploadFile.setEnabled(true);
                                binding.btnUploadFile.setText("Upload CSV");
                            });
                        }
                    });
                }
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnUploadFile.setEnabled(true);
                    binding.btnUploadFile.setText("Upload CSV");
                    Toast.makeText(this, "Error processing file", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private java.util.List<String> parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (line == null || line.isEmpty()) return result;
        
        boolean inQuotes = false;
        StringBuilder currentToken = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentToken.toString().trim());
                currentToken.setLength(0);
            } else {
                currentToken.append(c);
            }
        }
        result.add(currentToken.toString().trim());
        return result;
    }
}
