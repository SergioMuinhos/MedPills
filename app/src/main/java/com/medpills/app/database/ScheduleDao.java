package com.medpills.app.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ScheduleDao {
    @Insert
    long insert(Schedule schedule);

    @Update
    void update(Schedule schedule);

    @Delete
    void delete(Schedule schedule);

    @Query("SELECT * FROM schedules WHERE medication_id = :medicationId")
    List<Schedule> getSchedulesByMedication(long medicationId);

    @Query("SELECT * FROM schedules")
    List<Schedule> getAllSchedules();
}
