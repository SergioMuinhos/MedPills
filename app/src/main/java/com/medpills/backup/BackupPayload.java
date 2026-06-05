package com.medpills.backup;

import com.medpills.database.IntakeLog;
import com.medpills.database.Medication;
import com.medpills.database.Profile;
import com.medpills.database.Schedule;
import java.util.List;

public class BackupPayload {
    public List<Profile> profiles;
    public List<Medication> medications;
    public List<Schedule> schedules;
    public List<IntakeLog> intakeLogs;
}
