package com.medpills.app.database;

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

        profileDao.delete(profile);
        assertTrue(medicationDao.getAllMedications().isEmpty());
    }
}
