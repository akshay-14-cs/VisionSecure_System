package com.example.vs_project.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vs_project.R;
import com.example.vs_project.databinding.ActivityAnalyticsDashboardBinding;
import com.example.vs_project.databinding.LayoutBottomNavigationBinding;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalyticsDashboardActivity extends AppCompatActivity {

    private ActivityAnalyticsDashboardBinding binding;
    private FirebaseFirestore db;
    private String orgId = "";

    
    private LogAdapter regAdapter;
    private LogAdapter bioAdapter;
    private LogAdapter securityAdapter;
    private LogAdapter attendanceAdapter;

    private List<LogItem> regList = new ArrayList<>();
    private List<LogItem> bioList = new ArrayList<>();
    private List<LogItem> securityList = new ArrayList<>();
    private List<LogItem> attendanceList = new ArrayList<>();
    
    private List<LogItem> allRegList = new ArrayList<>();
    private List<LogItem> allBioList = new ArrayList<>();
    private List<LogItem> allAttendanceList = new ArrayList<>();
    
    private java.util.HashMap<String, String> studentNameMap = new java.util.HashMap<>();
    
    private String currentSearchQuery = "";
    private String currentRegFilter = "All";
    private String currentBioFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyticsDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null && getIntent().hasExtra("ORG_ID")) {
            orgId = getIntent().getStringExtra("ORG_ID");
        }

        db = FirebaseFirestore.getInstance();

        db = FirebaseFirestore.getInstance();

        
        

        setupTabs();
        setupRecyclerViews();
        setupChartListeners();
        setupSearchListener();
        fetchData();
        
        
        Intent serviceIntent = new Intent(this, IntruderAlertService.class);
        serviceIntent.putExtra("ORG_ID", orgId);
        startService(serviceIntent);

        
        if (getIntent() != null && getIntent().hasExtra("SELECTED_TAB")) {
            int selectedTab = getIntent().getIntExtra("SELECTED_TAB", 0);
            TabLayout.Tab tab = binding.tabLayout.getTabAt(selectedTab);
            if (tab != null) {
                tab.select();
                binding.llRegistrations.setVisibility(View.GONE);
                binding.llBiometrics.setVisibility(View.GONE);
                binding.llSecurity.setVisibility(View.GONE);
                binding.llAttendance.setVisibility(View.GONE);
                
                switch (selectedTab) {
                    case 0: binding.llRegistrations.setVisibility(View.VISIBLE); break;
                    case 1: binding.llBiometrics.setVisibility(View.VISIBLE); break;
                    case 2: binding.llSecurity.setVisibility(View.VISIBLE); break;
                    case 3: binding.llAttendance.setVisibility(View.VISIBLE); break;
                }
            }
        }

    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.llRegistrations.setVisibility(View.GONE);
                binding.llBiometrics.setVisibility(View.GONE);
                binding.llSecurity.setVisibility(View.GONE);
                binding.llAttendance.setVisibility(View.GONE);

                switch (tab.getPosition()) {
                    case 0: binding.llRegistrations.setVisibility(View.VISIBLE); break;
                    case 1: binding.llBiometrics.setVisibility(View.VISIBLE); break;
                    case 2: binding.llSecurity.setVisibility(View.VISIBLE); break;
                    case 3: binding.llAttendance.setVisibility(View.VISIBLE); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupChartListeners() {
        binding.chartRegistrations.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof PieEntry) {
                    String label = ((PieEntry) e).getLabel();
                    filterRegList(label);
                }
            }
            @Override
            public void onNothingSelected() {
                filterRegList("All");
            }
        });

        binding.chartBiometrics.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e.getX() == 0f) {
                    filterBioList("Completed");
                } else if (e.getX() == 1f) {
                    filterBioList("Pending");
                }
            }
            @Override
            public void onNothingSelected() {
                filterBioList("All");
            }
        });
    }

    private void setupSearchListener() {
        if (binding.etSearch != null) {
            binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    currentSearchQuery = s.toString().toLowerCase().trim();
                    applyFilters();
                }
            });
        }
    }

    private void filterRegList(String filter) {
        currentRegFilter = filter;
        applyFilters();
    }

    private void filterBioList(String filter) {
        currentBioFilter = filter;
        applyFilters();
    }
    
    private void applyFilters() {
        
        regList.clear();
        for (LogItem item : allRegList) {
            boolean matchesChart = currentRegFilter.equals("All") 
                || (currentRegFilter.equals("Uploaded") && item.title.contains("Complete"))
                || (currentRegFilter.equals("Pending") && item.title.contains("Pending"));
                
            boolean matchesSearch = currentSearchQuery.isEmpty() 
                || (item.title != null && item.title.toLowerCase().contains(currentSearchQuery))
                || (item.details != null && item.details.toLowerCase().contains(currentSearchQuery));
                
            if (matchesChart && matchesSearch) {
                regList.add(item);
            }
        }
        regAdapter.notifyDataSetChanged();
        
        
        bioList.clear();
        for (LogItem item : allBioList) {
            boolean matchesChart = currentBioFilter.equals("All") 
                || (currentBioFilter.equals("Completed") && item.title.contains("Complete"))
                || (currentBioFilter.equals("Pending") && item.title.contains("Pending"));
                
            boolean matchesSearch = currentSearchQuery.isEmpty() 
                || (item.title != null && item.title.toLowerCase().contains(currentSearchQuery))
                || (item.details != null && item.details.toLowerCase().contains(currentSearchQuery));
                
            if (matchesChart && matchesSearch) {
                bioList.add(item);
            }
        }
        bioAdapter.notifyDataSetChanged();
        
        
        attendanceList.clear();
        for (LogItem item : allAttendanceList) {
            boolean matchesSearch = currentSearchQuery.isEmpty() 
                || (item.title != null && item.title.toLowerCase().contains(currentSearchQuery))
                || (item.details != null && item.details.toLowerCase().contains(currentSearchQuery));
                
            if (matchesSearch) {
                attendanceList.add(item);
            }
        }
        if (attendanceAdapter != null) attendanceAdapter.notifyDataSetChanged();
    }
    private void setupRecyclerViews() {
        regAdapter = new LogAdapter(regList, false, null);
        binding.rvRegistrations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRegistrations.setAdapter(regAdapter);

        bioAdapter = new LogAdapter(bioList, false, null);
        binding.rvBiometrics.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBiometrics.setAdapter(bioAdapter);

        securityAdapter = new LogAdapter(securityList, true, this::showImageDialog);
        binding.rvSecurityLogs.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSecurityLogs.setAdapter(securityAdapter);

        attendanceAdapter = new LogAdapter(attendanceList, false, null);
        if (binding.rvAttendance != null) {
            binding.rvAttendance.setLayoutManager(new LinearLayoutManager(this));
            binding.rvAttendance.setAdapter(attendanceAdapter);
        }
    }

    private void fetchData() {
        if (orgId == null || orgId.isEmpty()) return;

        
        db.collection("students")
          .whereEqualTo("org_id", orgId)
          .addSnapshotListener((value, error) -> {
              if (error != null) return;
              if (value != null) {
                  int total = value.size();
                  int bioComplete = 0;
                  int examComplete = 0;
                  
                  allRegList.clear();
                  allBioList.clear();

                  SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

                  for (QueryDocumentSnapshot doc : value) {
                      String name = doc.getString("name");
                      String id = doc.getString("student_id");
                      
                      if (id != null) {
                          studentNameMap.put(id, name != null ? name : "Unknown");
                      }
                      
                      Boolean hasFace = doc.getBoolean("has_face_registered");
                      if (hasFace == null) hasFace = false;
                      
                      Boolean isExamReg = doc.getBoolean("exam_registered");
                      if (isExamReg == null) isExamReg = false;
                      
                      com.google.firebase.Timestamp tsReg = doc.getTimestamp("exam_registered_at");
                      String timeStrReg = tsReg != null ? sdf.format(tsReg.toDate()) : "N/A";
                      
                      
                      if (isExamReg) {
                          examComplete++;
                          allRegList.add(new LogItem(
                              "Exam Registration Complete",
                              "Student: " + name + " (ID: " + id + ")",
                              timeStrReg + " | Registered by: " + name, null
                          ));
                      } else {
                          allRegList.add(new LogItem(
                              "Pending Exam Registration",
                              "Student: " + name + " (ID: " + id + ")",
                              "Action Required", null
                          ));
                      }
                      
                      
                      if (hasFace) {
                          bioComplete++;
                          allBioList.add(new LogItem(
                              "Face Registration Complete",
                              "Student: " + name + " (ID: " + id + ")",
                              "Completed | Registered by: " + name, null
                          ));
                      } else {
                          allBioList.add(new LogItem(
                              "Pending Biometrics",
                              "Student: " + name + " (ID: " + id + ")",
                              "Action Required", null
                          ));
                      }
                  }
                  
                  
                  filterRegList("All");
                  filterBioList("All");
                  
                  updateRegChart(examComplete, total - examComplete);
                  updateBioChart(bioComplete, total - bioComplete);
              }
          });
          
        
        db.collection("intruder_records")
          .whereEqualTo("org_id", orgId)
          .addSnapshotListener((value, error) -> {
              if (error != null) return;
              if (value != null) {
                  securityList.clear();
                  for (QueryDocumentSnapshot doc : value) {
                      String title = doc.getString("title");
                      String details = doc.getString("details");
                      String time = doc.getString("timestamp");
                      String imgBase64 = doc.getString("intruder_image_base64");
                      securityList.add(new LogItem(
                          title != null ? title : "Intruder Alert",
                          details != null ? details : "Face mismatch detected.",
                          time != null ? time : new Date().toString(),
                          imgBase64
                      ));
                  }
                  securityAdapter.notifyDataSetChanged();
              }
          });
          
        
        db.collection("attendance")
          .whereEqualTo("org_id", orgId)
          .addSnapshotListener((value, error) -> {
              if (error != null) return;
              if (value != null) {
                  allAttendanceList.clear();
                  for (QueryDocumentSnapshot doc : value) {
                      String stuId = doc.getString("student_id");
                      String subCode = doc.getString("subject_code");
                      String date = doc.getString("exam_date");
                      String time = doc.getString("exam_time");
                      String verifyTime = doc.getString("verification_timestamp");
                      String status = doc.getString("status");
                      
                      String stuName = studentNameMap.containsKey(stuId) ? studentNameMap.get(stuId) : "Unknown Student";
                      
                      String title = (subCode != null ? subCode : "Unknown Sub") + " - " + (date != null ? date : "");
                      String details = "Student: " + stuName + " (ID: " + stuId + ") - Status: " + (status != null ? status : "N/A");
                      String timestamp = "Verified at: " + (verifyTime != null ? verifyTime : "N/A");
                      
                      allAttendanceList.add(new LogItem(title, details, timestamp, null));
                  }
                  filterBioList(currentBioFilter); 
                  filterRegList(currentRegFilter); 
                  
                  applyFilters();
              }
          });
    }

    private void updateRegChart(int complete, int pending) {
        List<PieEntry> entries = new ArrayList<>();
        if (complete > 0) entries.add(new PieEntry(complete, "Uploaded"));
        if (pending > 0) entries.add(new PieEntry(pending, "Pending"));
        
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Data"));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Registration Status");
        if (entries.size() == 1 && entries.get(0).getLabel().equals("No Data")) {
            dataSet.setColors(Color.parseColor("#E0E0E0"));
        } else {
            dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"));
        }
        PieData data = new PieData(dataSet);
        binding.chartRegistrations.setData(data);
        binding.chartRegistrations.invalidate(); 
    }

    private void updateBioChart(int complete, int pending) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, complete));
        entries.add(new BarEntry(1f, pending));
        
        BarDataSet dataSet = new BarDataSet(entries, "Biometrics Status");
        dataSet.setColors(Color.parseColor("#2196F3"), Color.parseColor("#F44336"));
        BarData data = new BarData(dataSet);
        binding.chartBiometrics.setData(data);
        binding.chartBiometrics.invalidate();
    }

    

    
    private void showImageDialog(String title, String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) return;
        try {
            byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            ImageView iv = new ImageView(this);
            iv.setImageBitmap(decodedByte);
            iv.setAdjustViewBounds(true);
            iv.setPadding(16, 16, 16, 16);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(title)
                    .setView(iv)
                    .setPositiveButton("Close", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }
}
