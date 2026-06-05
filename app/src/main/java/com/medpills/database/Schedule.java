package com.medpills.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "schedules",
    foreignKeys = @ForeignKey(
        entity = Medication.class,
        parentColumns = "id",
        childColumns = "medication_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("medication_id")}
)
public class Schedule {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "medication_id")
    private long medicationId;

    @ColumnInfo(name = "frequency_type")
    private String frequencyType; // 'DAILY', 'INTERVAL', 'SPECIFIC_DAYS', 'CYCLIC'

    @ColumnInfo(name = "interval_hours")
    private int intervalHours;

    @ColumnInfo(name = "days_of_week")
    private String daysOfWeek; // e.g. "MONDAY,TUESDAY"

    @ColumnInfo(name = "cycle_on_days")
    private int cycleOnDays;

    @ColumnInfo(name = "cycle_off_days")
    private int cycleOffDays;

    @ColumnInfo(name = "start_date_millis")
    private long startDateMillis;

    @ColumnInfo(name = "end_date_millis")
    private Long endDateMillis; // Nullable for ongoing

    @ColumnInfo(name = "target_times")
    private String targetTimes; // e.g. "08:00,16:00"

    // Constructors
    public Schedule() {}

    public Schedule(long medicationId, String frequencyType, int intervalHours, String daysOfWeek, int cycleOnDays, int cycleOffDays, long startDateMillis, String targetTimes) {
        this.medicationId = medicationId;
        this.frequencyType = frequencyType;
        this.intervalHours = intervalHours;
        this.daysOfWeek = daysOfWeek;
        this.cycleOnDays = cycleOnDays;
        this.cycleOffDays = cycleOffDays;
        this.startDateMillis = startDateMillis;
        this.targetTimes = targetTimes;
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMedicationId() { return medicationId; }
    public void setMedicationId(long medicationId) { this.medicationId = medicationId; }

    public String getFrequencyType() { return frequencyType; }
    public void setFrequencyType(String frequencyType) { this.frequencyType = frequencyType; }

    public int getIntervalHours() { return intervalHours; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public int getCycleOnDays() { return cycleOnDays; }
    public void setCycleOnDays(int cycleOnDays) { this.cycleOnDays = cycleOnDays; }

    public int getCycleOffDays() { return cycleOffDays; }
    public void setCycleOffDays(int cycleOffDays) { this.cycleOffDays = cycleOffDays; }

    public long getStartDateMillis() { return startDateMillis; }
    public void setStartDateMillis(long startDateMillis) { this.startDateMillis = startDateMillis; }

    public Long getEndDateMillis() { return endDateMillis; }
    public void setEndDateMillis(Long endDateMillis) { this.endDateMillis = endDateMillis; }

    public String getTargetTimes() { return targetTimes; }
    public void setTargetTimes(String targetTimes) { this.targetTimes = targetTimes; }
}
