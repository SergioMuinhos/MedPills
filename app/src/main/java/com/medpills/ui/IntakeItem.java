package com.medpills.ui;

import com.medpills.database.Medication;
import com.medpills.database.Schedule;

public class IntakeItem {
    private final Schedule schedule;
    private final Medication medication;
    private final long scheduledTimeMillis;
    private String status; // "PENDING", "TAKEN", "SKIPPED", "POSTPONED"
    private long logId = -1;

    public IntakeItem(Schedule schedule, Medication medication, long scheduledTimeMillis, String status) {
        this.schedule = schedule;
        this.medication = medication;
        this.scheduledTimeMillis = scheduledTimeMillis;
        this.status = status;
    }

    public Schedule getSchedule() { return schedule; }
    public Medication getMedication() { return medication; }
    public long getScheduledTimeMillis() { return scheduledTimeMillis; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getLogId() { return logId; }
    public void setLogId(long logId) { this.logId = logId; }
}
