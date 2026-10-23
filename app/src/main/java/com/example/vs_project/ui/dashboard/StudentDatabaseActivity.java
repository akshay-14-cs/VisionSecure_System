package com.example.vs_project.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vs_project.R;
import com.example.vs_project.databinding.ActivityStudentDatabaseBinding;
import com.example.vs_project.databinding.LayoutBottomNavigationBinding;
import android.content.Intent;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AutoCompleteTextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import androidx.appcompat.app.AlertDialog;
import android.widget.ImageView;
import androidx.recyclerview.widget.GridLayoutManager;
import android.widget.AdapterView;

public class StudentDatabaseActivity extends AppCompatActivity {

    private ActivityStudentDatabaseBinding binding;
    private FirebaseFirestore db;
    private StudentAdapter adapter;
    private List<Student> studentList;
    private List<Student> filteredList;
    private String orgId = "";
    private String userRole = "";
    private String facultyDepartment = "";
    private boolean isGridView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDatabaseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            orgId = getIntent().getStringExtra("ORG_ID") != null ? getIntent().getStringExtra("ORG_ID") : "";
            userRole = getIntent().getStringExtra("USER_ROLE") != null ? getIntent().getStringExtra("USER_ROLE") : "";
            facultyDepartment = getIntent().getStringExtra("DEPARTMENT") != null ? getIntent().getStringExtra("DEPARTMENT") : "";
        }

        db = FirebaseFirestore.getInstance();
        studentList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new StudentAdapter(filteredList);
        binding.rvStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStudents.setAdapter(adapter);

        
        binding.btnToggleLayout.setOnClickListener(v -> {
            isGridView = !isGridView;
            if (isGridView) {
                binding.btnToggleLayout.setImageResource(android.R.drawable.ic_menu_sort_by_size); 
                binding.rvStudents.setLayoutManager(new GridLayoutManager(this, 2));
            } else {
                binding.btnToggleLayout.setImageResource(android.R.drawable.ic_menu_sort_by_size); 
                binding.rvStudents.setLayoutManager(new LinearLayoutManager(this));
            }
            adapter.notifyDataSetChanged(); 
        });

        
        setupFilters();

        setupRealtimeListener();

        
        binding.etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

    }

    private void setupFilters() {
        String[] backlogs = {"All", "Has Active Backlogs", "No Backlogs"};
        ArrayAdapter<String> backlogAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, backlogs);
        backlogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBacklogs.setAdapter(backlogAdapter);

        binding.spinnerBacklogs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        binding.spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        
        if ("faculty".equalsIgnoreCase(userRole) || "incharge".equalsIgnoreCase(userRole)) {
            binding.spinnerDepartment.setEnabled(false);
            binding.spinnerDepartment.setClickable(false);
        }
    }

    private void updateDepartmentSpinner() {
        List<String> depts = new ArrayList<>();
        
        if ("faculty".equalsIgnoreCase(userRole) || "incharge".equalsIgnoreCase(userRole)) {
            depts.add(facultyDepartment != null && !facultyDepartment.isEmpty() ? facultyDepartment : "Your Department");
        } else {
            depts.add("All Departments");
            for (Student s : studentList) {
                if (!depts.contains(s.department) && !s.department.isEmpty()) {
                    depts.add(s.department);
                }
            }
        }
        
        String currentSelection = binding.spinnerDepartment.getSelectedItem() != null ? 
                                  binding.spinnerDepartment.getSelectedItem().toString() : depts.get(0);

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, depts);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDepartment.setAdapter(deptAdapter);
        
        int position = depts.indexOf(currentSelection);
        if (position >= 0) binding.spinnerDepartment.setSelection(position);
    }

    private void updateAutoComplete() {
        List<String> suggestions = new ArrayList<>();
        for (Student s : studentList) {
            suggestions.add(s.name);
            suggestions.add(s.studentId);
        }
        ArrayAdapter<String> actvAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
        binding.etSearchStudent.setAdapter(actvAdapter);
    }

    private void setupRealtimeListener() {
        if (orgId.isEmpty()) return;

        db.collection("students")
          .whereEqualTo("org_id", orgId)
          .addSnapshotListener((value, error) -> {
              if (error != null) {
                  Toast.makeText(this, "Error fetching students: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                  return;
              }

              if (value != null) {
                  studentList.clear();
                  for (QueryDocumentSnapshot doc : value) {
                      String name = doc.getString("name");
                      String studentId = doc.getString("student_id");
                      String dept = doc.getString("department");
                      String courses = doc.getString("courses");
                      String backlogsStr = doc.getString("backlogs");
                      String backlogCourses = doc.getString("backlog_courses");
                      String imageBase64 = doc.getString("face_image_base64");
                      
                      int backlogs = 0;
                      try {
                          if (backlogsStr != null) backlogs = Integer.parseInt(backlogsStr.trim());
                      } catch (NumberFormatException e) { e.printStackTrace(); }

                      studentList.add(new Student(
                          name != null ? name : "",
                          studentId != null ? studentId : "",
                          dept != null ? dept : "",
                          courses != null ? courses : "",
                          backlogs,
                          backlogCourses != null ? backlogCourses : "",
                          imageBase64 != null ? imageBase64 : ""
                      ));
                  }
                  
                  updateDepartmentSpinner();
                  updateAutoComplete();
                  applyFilters();
              }
          });
    }

    private void applyFilters() {
        filteredList.clear();
        String query = binding.etSearchStudent.getText() != null ? binding.etSearchStudent.getText().toString().toLowerCase().trim() : "";
        String deptFilter = binding.spinnerDepartment.getSelectedItem() != null ? binding.spinnerDepartment.getSelectedItem().toString() : "All Departments";
        String backlogFilter = binding.spinnerBacklogs.getSelectedItem() != null ? binding.spinnerBacklogs.getSelectedItem().toString() : "All";

        for (Student s : studentList) {
            boolean matchesSearch = query.isEmpty() || s.name.toLowerCase().contains(query) || s.studentId.toLowerCase().contains(query);
            boolean matchesDept = deptFilter.equals("All Departments") || s.department.equalsIgnoreCase(deptFilter);
            boolean matchesBacklog = backlogFilter.equals("All") || 
                                     (backlogFilter.equals("Has Active Backlogs") && s.backlogs > 0) ||
                                     (backlogFilter.equals("No Backlogs") && s.backlogs == 0);

            if (matchesSearch && matchesDept && matchesBacklog) {
                filteredList.add(s);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showImageDialog(String name, String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            Toast.makeText(this, "No image registered for this student.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(decodedByte);
            imageView.setPadding(32, 32, 32, 32);

            new AlertDialog.Builder(this)
                .setTitle("Registered Face: " + name)
                .setView(imageView)
                .setPositiveButton("Close", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    

    public static class Student {
        String name, studentId, department, courses, backlogCourses, imageBase64;
        int backlogs;

        public Student(String name, String studentId, String department, String courses, int backlogs, String backlogCourses, String imageBase64) {
            this.name = name;
            this.studentId = studentId;
            this.department = department;
            this.courses = courses;
            this.backlogs = backlogs;
            this.backlogCourses = backlogCourses;
            this.imageBase64 = imageBase64;
        }
    }

    public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

        private List<Student> list;

        public StudentAdapter(List<Student> list) {
            this.list = list;
        }

        @Override
        public int getItemViewType(int position) {
            return isGridView ? 1 : 0;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v;
            if (viewType == 1) {
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_grid, parent, false);
            } else {
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
            }
            return new StudentViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            Student s = list.get(position);
            holder.tvName.setText(s.name);
            holder.tvId.setText("ID: " + s.studentId);
            holder.tvDept.setText(s.department);
            holder.tvCourses.setText(s.courses);
            holder.chipBacklogs.setText("Backlogs: " + s.backlogs);

            if (s.backlogs > 0) {
                holder.cvStudent.setStrokeWidth(4);
                holder.cvStudent.setStrokeColor(Color.parseColor("#EF9A9A"));
                holder.chipBacklogs.setChipBackgroundColorResource(android.R.color.holo_red_light);
                holder.chipBacklogs.setTextColor(Color.WHITE);
                if (holder.llBacklogCourses != null) {
                    holder.llBacklogCourses.setVisibility(View.VISIBLE);
                    holder.tvBacklogCourses.setText(s.backlogCourses);
                }
            } else {
                holder.cvStudent.setStrokeWidth(1);
                holder.cvStudent.setStrokeColor(Color.parseColor("#1A000000"));
                holder.chipBacklogs.setChipBackgroundColorResource(R.color.surfaceColor);
                holder.chipBacklogs.setTextColor(Color.parseColor("#757575")); 
                if (holder.llBacklogCourses != null) {
                    holder.llBacklogCourses.setVisibility(View.GONE);
                }
            }
            
            if (holder.btnViewImage != null) {
                holder.btnViewImage.setOnClickListener(v -> showImageDialog(s.name, s.imageBase64));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvId, tvDept, tvCourses, tvBacklogCourses;
            com.google.android.material.chip.Chip chipBacklogs;
            com.google.android.material.card.MaterialCardView cvStudent;
            View llBacklogCourses;
            com.google.android.material.button.MaterialButton btnViewImage;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvStudentName);
                tvId = itemView.findViewById(R.id.tvStudentId);
                tvDept = itemView.findViewById(R.id.tvDepartment);
                tvCourses = itemView.findViewById(R.id.tvCourses);
                chipBacklogs = itemView.findViewById(R.id.chipBacklogs);
                cvStudent = itemView.findViewById(R.id.cvStudent);
                tvBacklogCourses = itemView.findViewById(R.id.tvBacklogCourses);
                llBacklogCourses = itemView.findViewById(R.id.llBacklogCourses);
                btnViewImage = itemView.findViewById(R.id.btnViewImage);
            }
        }
    }
}
