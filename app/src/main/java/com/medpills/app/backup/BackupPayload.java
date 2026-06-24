package com.medpills.app.backup;

import com.medpills.app.database.IntakeLog;
import com.medpills.app.database.Medication;
import com.medpills.app.database.Profile;
import com.medpills.app.database.Schedule;
import java.util.List;

public class BackupPayload {
    public List<Profile> profiles;
    public List<Medication> medications;
    public List<Schedule> schedules;
    public List<IntakeLog> intakeLogs;
}
