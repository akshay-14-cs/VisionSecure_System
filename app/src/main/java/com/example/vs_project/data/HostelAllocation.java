package com.example.vs_project.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hostel_allocations")
public class HostelAllocation {

    @PrimaryKey
    @NonNull
    public String studentId;

    public String orgId;
    public String roomNo;
    public String blockName;

    public HostelAllocation(@NonNull String studentId, String orgId, String roomNo, String blockName) {
        this.studentId = studentId;
        this.orgId = orgId;
        this.roomNo = roomNo;
        this.blockName = blockName;
    }
}
