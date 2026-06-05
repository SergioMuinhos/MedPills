package com.medpills.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ProfileDao {
    @Insert
    long insert(Profile profile);

    @Query("SELECT COUNT(*) FROM profiles")
    int getProfileCount();

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    List<Profile> getAllProfiles();

    @Query("SELECT * FROM profiles WHERE id = :id")
    Profile getProfileById(long id);

    @Delete
    void delete(Profile profile);

    @androidx.room.Update
    void update(Profile profile);
}
