package com.medpills.database;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {
    private AppDatabase db;
    private ProfileDao profileDao;
    private MedicationDao medicationDao;
    private IntakeLogDao intakeLogDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        // Create an in-memory database for testing (fast, isolated, and reset on close)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        profileDao = db.profileDao();
        medicationDao = db.medicationDao();
        intakeLogDao = db.intakeLogDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void testProfileInsertionAndRetrieval() {
        Profile profile = new Profile("Juan Perez", "avatar_1");
        profile.setId(10);
        profileDao.insert(profile);

        List<Profile> all = profileDao.getAllProfiles();
        assertEquals(1, all.size());
        assertEquals("Juan Perez", all.get(0).getName());
        assertEquals("avatar_1", all.get(0).getAvatarResourceName());
    }

    @Test
    public void testMedicationInsertionAndCascade() {
        Profile profile = new Profile("User A", "avatar_default");
        profile.setId(1);
        profileDao.insert(profile);

        Medication med = new Medication(1, "Ibuprofeno 600", 1.0, "pastilla", 20, 5);
        med.setId(100);
        medicationDao.insert(med);

        List<Medication> meds = medicationDao.getAllMedications();
        assertEquals(1, meds.size());
        assertEquals("Ibuprofeno 600", meds.get(0).getName());

        // Test delete cascade: deleting the profile should delete the medication
        profileDao.delete(profile);
        assertTrue(medicationDao.getAllMedications().isEmpty());
    }

    @Test
    public void testOverdoseGuardTrigger() {
        Profile profile = new Profile("User B", "avatar_default");
        profile.setId(2);
        profileDao.insert(profile);

        Medication med = new Medication(2, "Paracetamol 1g", 1.0, "pastilla", 30, 3);
        med.setId(200);
        medicationDao.insert(med);

        long now = System.currentTimeMillis();

        // 1. Insert the first intake log: should succeed
        IntakeLog log1 = new IntakeLog(200, 2, now, now, "TAKEN");
        try {
            intakeLogDao.insertWithOverdoseGuard(log1);
        } catch (OverdoseException e) {
            fail("First intake log should not trigger an overdose alert");
        }

        // 2. Insert a second intake log 30 minutes later: should throw OverdoseException
        long thirtyMinutesLater = now + (30 * 60 * 1000);
        IntakeLog log2 = new IntakeLog(200, 2, thirtyMinutesLater, thirtyMinutesLater, "TAKEN");
        
        try {
            intakeLogDao.insertWithOverdoseGuard(log2);
            fail("Second intake log within 90 minutes window should trigger OverdoseException");
        } catch (OverdoseException e) {
            // Expected exception
            assertNotNull(e.getMessage());
        }

        // 3. Insert a third intake log 100 minutes later: should succeed (outside the 90 minutes window)
        long hundredMinutesLater = now + (100 * 60 * 1000);
        IntakeLog log3 = new IntakeLog(200, 2, hundredMinutesLater, hundredMinutesLater, "TAKEN");
        try {
            intakeLogDao.insertWithOverdoseGuard(log3);
        } catch (OverdoseException e) {
            fail("Intake log outside 90 minutes window should not trigger overdose alert");
        }
    }

    @Test
    public void testOverdoseForceBypass() {
        Profile profile = new Profile("User C", "avatar_default");
        profile.setId(3);
        profileDao.insert(profile);

        Medication med = new Medication(3, "Aspirina", 1.0, "pastilla", 10, 2);
        med.setId(300);
        medicationDao.insert(med);

        long now = System.currentTimeMillis();

        IntakeLog log1 = new IntakeLog(300, 3, now, now, "TAKEN");
        intakeLogDao.insertWithOverdoseForce(log1);

        // Forced intake log 5 minutes later: should succeed because we use force bypass
        long fiveMinutesLater = now + (5 * 60 * 1000);
        IntakeLog log2 = new IntakeLog(300, 3, fiveMinutesLater, fiveMinutesLater, "TAKEN");
        intakeLogDao.insertWithOverdoseForce(log2);

        List<IntakeLog> logs = intakeLogDao.getAllLogs();
        assertEquals(2, logs.size());
    }
}
