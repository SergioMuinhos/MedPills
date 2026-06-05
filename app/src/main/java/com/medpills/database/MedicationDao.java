package com.medpills.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MedicationDao {
    @Insert
    long insert(Medication medication);

    @Update
    void update(Medication medication);

    @Delete
    void delete(Medication medication);

    @Query("SELECT * FROM medications ORDER BY name ASC")
    List<Medication> getAllMedications();

    @Query("SELECT * FROM medications WHERE profile_id = :profileId ORDER BY name ASC")
    List<Medication> getMedicationsByProfile(long profileId);

    @Query("SELECT * FROM medications WHERE id = :id")
    Medication getMedicationById(long id);
}
