package com.example.vs_project.ui.dashboard;

import com.google.firebase.firestore.PropertyName;

public class TimetableSlot {
    public String date;
    public String timing;
    
    @PropertyName("subject_code")
    public String subjectCode;
    
    @PropertyName("hall_no")
    public String hallNo;
    
    @PropertyName("faculty_id")
    public String facultyId;
    
    @PropertyName("exam_mode")
    public String examMode;

    public TimetableSlot() {
        
    }

    public TimetableSlot(String date, String timing, String subjectCode, String hallNo, String facultyId, String examMode) {
        this.date = date;
        this.timing = timing;
        this.subjectCode = subjectCode;
        this.hallNo = hallNo;
        this.facultyId = facultyId;
        this.examMode = examMode;
    }
}
