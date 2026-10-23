package com.example.vs_project.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "timetable")
public class TimetableEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String courseCode;
    public String date;
    public String time;
    public String roomNo;
    public String invigilatorId;
    public String orgId;
}
