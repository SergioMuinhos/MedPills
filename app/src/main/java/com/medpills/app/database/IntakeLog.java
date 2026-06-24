package com.medpills.app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "intake_logs",
    foreignKeys = {
        @ForeignKey(
            entity = Medication.class,
            parentColumns = "id",
            childColumns = "medication_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Profile.class,
            parentColumns = "id",
            childColumns = "profile_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("medication_id"),
        @Index("profile_id")
    }
)
public class IntakeLog {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "medication_id")
    private long medicationId;

    @ColumnInfo(name = "profile_id")
    private long profileId;

    @ColumnInfo(name = "timestamp_millis")
    private long timestampMillis; // Actual time of action

    @ColumnInfo(name = "scheduled_time_millis")
    private long scheduledTimeMillis; // Time it was supposed to happen

    @ColumnInfo(name = "status")
    private String status; // 'TAKEN', 'SKIPPED', 'POSTPONED'

    // Constructors
    public IntakeLog() {}

    public IntakeLog(long medicationId, long profileId, long timestampMillis, String status) {
        this.medicationId = medicationId;
        this.profileId = profileId;
        this.timestampMillis = timestampMillis;
        this.status = status;
        this.scheduledTimeMillis = timestampMillis; // Default to same if not specified
    }

    public IntakeLog(long medicationId, long profileId, long timestampMillis, long scheduledTimeMillis, String status) {
        this.medicationId = medicationId;
        this.profileId = profileId;
        this.timestampMillis = timestampMillis;
        this.scheduledTimeMillis = scheduledTimeMillis;
        this.status = status;
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMedicationId() { return medicationId; }
    public void setMedicationId(long medicationId) { this.medicationId = medicationId; }

    public long getProfileId() { return profileId; }
    public void setProfileId(long profileId) { this.profileId = profileId; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public long getScheduledTimeMillis() { return scheduledTimeMillis; }
    public void setScheduledTimeMillis(long scheduledTimeMillis) { this.scheduledTimeMillis = scheduledTimeMillis; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
