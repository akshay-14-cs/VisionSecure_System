package com.example.vs_project.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TimetableDao {

    @Insert
    void insertAll(List<TimetableEntity> timetables);

    @Query("SELECT * FROM timetable WHERE orgId = :orgId")
    List<TimetableEntity> getTimetablesForOrg(String orgId);
    
    @Query("DELETE FROM timetable")
    void clearAll();
}
