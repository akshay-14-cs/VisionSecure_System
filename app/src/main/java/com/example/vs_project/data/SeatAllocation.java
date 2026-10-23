package com.example.vs_project.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "seat_allocations")
public class SeatAllocation {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String studentId;

    public String orgId;
    public String examId;
    public String seatNo;

    public SeatAllocation(@NonNull String studentId, String orgId, String examId, String seatNo) {
        this.studentId = studentId;
        this.orgId = orgId;
        this.examId = examId;
        this.seatNo = seatNo;
    }
}
