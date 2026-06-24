package com.medpills.backup;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.gson.Gson;
import com.medpills.database.AppDatabase;
import com.medpills.database.DatabaseClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BackupManagerTest {
    private AppDatabase db;

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        
        // Mock DatabaseClient instance using reflection
        DatabaseClient client = DatabaseClient.getInstance(context);
        Field dbField = DatabaseClient.class.getDeclaredField("appDatabase");
        dbField.setAccessible(true);
        dbField.set(client, db);
    }

    @Test
    public void testParseAndImportBackupJson() throws Exception {
        File file = new File("../medpills_backup.json");
        assertTrue("Backup file should exist", file.exists());
        String jsonStr = new String(Files.readAllBytes(file.toPath()), "UTF-8");
        
        Gson gson = new Gson();
        BackupPayload payload = gson.fromJson(jsonStr, BackupPayload.class);
        assertNotNull("Payload should not be null", payload);
        
        // Run database import operations sequentially in the current thread (allowMainThreadQueries is active)
        db.runInTransaction(() -> {
            // Purge old data completely
            db.query("DELETE FROM intake_logs", null).close();
            db.query("DELETE FROM schedules", null).close();
            db.query("DELETE FROM medications", null).close();
            db.query("DELETE FROM profiles", null).close();

            // Insert profiles
            for (com.medpills.database.Profile profile : payload.profiles) {
                db.profileDao().insert(profile);
            }

            // Insert medications
            for (com.medpills.database.Medication med : payload.medications) {
                db.medicationDao().insert(med);
            }

            // Insert schedules
            for (com.medpills.database.Schedule sched : payload.schedules) {
                db.scheduleDao().insert(sched);
            }

            // Insert intake logs
            for (com.medpills.database.IntakeLog log : payload.intakeLogs) {
                db.intakeLogDao().insert(log);
            }
        });
        
        // Verify database counts
        assertEquals(payload.profiles.size(), db.profileDao().getAllProfiles().size());
        assertEquals(payload.medications.size(), db.medicationDao().getAllMedications().size());
        assertEquals(payload.schedules.size(), db.scheduleDao().getAllSchedules().size());
        assertEquals(payload.intakeLogs.size(), db.intakeLogDao().getAllLogs().size());
    }
}
