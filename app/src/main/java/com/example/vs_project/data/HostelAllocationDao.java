package com.example.vs_project.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HostelAllocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HostelAllocation> allocations);

    @Query("SELECT * FROM hostel_allocations WHERE orgId = :orgId AND studentId = :studentId")
    HostelAllocation getStudentRoom(String orgId, String studentId);
    
    @Query("DELETE FROM hostel_allocations WHERE orgId = :orgId")
    void clearAllocationsByOrg(String orgId);
}
