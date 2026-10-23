package com.example.vs_project.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SeatAllocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SeatAllocation> allocations);

    @Query("SELECT * FROM seat_allocations WHERE orgId = :orgId AND studentId = :studentId")
    List<SeatAllocation> getStudentExams(String orgId, String studentId);

    @Query("DELETE FROM seat_allocations WHERE orgId = :orgId AND examId = :examId")
    void clearAllocationsByExam(String orgId, String examId);
}
