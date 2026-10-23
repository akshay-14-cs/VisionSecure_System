package com.example.vs_project.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TimetableEntity.class, HostelAllocation.class, SeatAllocation.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TimetableDao timetableDao();
    public abstract HostelAllocationDao hostelAllocationDao();
    public abstract SeatAllocationDao seatAllocationDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "visionsecure_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
