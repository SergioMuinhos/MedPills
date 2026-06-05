package com.medpills.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public abstract class IntakeLogDao {
    @Insert
    public abstract long insert(IntakeLog log);

    @Query("SELECT * FROM intake_logs WHERE medication_id = :medicationId AND status = 'TAKEN' AND timestamp_millis >= :sinceTimeMillis")
    public abstract List<IntakeLog> getTakenLogsSince(long medicationId, long sinceTimeMillis);

    @Query("SELECT * FROM intake_logs ORDER BY timestamp_millis DESC")
    public abstract List<IntakeLog> getAllLogs();

    @Query("SELECT * FROM intake_logs WHERE profile_id = :profileId AND timestamp_millis BETWEEN :startMillis AND :endMillis ORDER BY timestamp_millis DESC")
    public abstract List<IntakeLog> getLogsFiltered(long profileId, long startMillis, long endMillis);

    @Transaction
    public long insertWithOverdoseGuard(IntakeLog log) throws OverdoseException {
        if ("TAKEN".equals(log.getStatus())) {
            // Check for previous intakes in a 90-minute rolling window
            long rollingWindowStart = log.getTimestampMillis() - (90 * 60 * 1000); // 90 minutes ago
            List<IntakeLog> recentLogs = getTakenLogsSince(log.getMedicationId(), rollingWindowStart);
            
            if (!recentLogs.isEmpty()) {
                throw new OverdoseException("Se ha registrado una toma de este medicamento recientemente (ventana de 90 min).");
            }
        }
        return insert(log);
    }

    @Transaction
    public long insertWithOverdoseForce(IntakeLog log) {
        return insert(log);
    }

    @Query("DELETE FROM intake_logs WHERE id = :logId")
    public abstract void deleteById(long logId);

    @Query("SELECT * FROM intake_logs WHERE scheduled_time_millis BETWEEN :startMillis AND :endMillis")
    public abstract List<IntakeLog> getLogsFilteredAcrossProfiles(long startMillis, long endMillis);
}
