package com.medpills.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

public class MedicationRepository {
    private final Context context;
    private final DatabaseClient dbClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnLogResultListener {
        void onSuccess(long logId);
        void onOverdoseWarning(IntakeLog log, String message);
        void onError(Exception e);
    }

    public interface OnResultListener<T> {
        void onResult(T result);
    }

    public MedicationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbClient = DatabaseClient.getInstance(context);
    }

    public void getAllProfiles(OnResultListener<List<Profile>> listener) {
        dbClient.executor().execute(() -> {
            List<Profile> profiles = dbClient.db().profileDao().getAllProfiles();
            mainHandler.post(() -> listener.onResult(profiles));
        });
    }

    public void insertProfile(Profile profile, Runnable onComplete) {
        dbClient.executor().execute(() -> {
            dbClient.db().profileDao().insert(profile);
            com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void deleteProfile(Profile profile, Runnable onComplete) {
        dbClient.executor().execute(() -> {
            dbClient.db().profileDao().delete(profile);
            com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void updateProfile(Profile profile, Runnable onComplete) {
        dbClient.executor().execute(() -> {
            dbClient.db().profileDao().update(profile);
            com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void getMedicationsByProfile(long profileId, OnResultListener<List<Medication>> listener) {
        dbClient.executor().execute(() -> {
            List<Medication> meds = dbClient.db().medicationDao().getMedicationsByProfile(profileId);
            mainHandler.post(() -> listener.onResult(meds));
        });
    }

    public void getSchedulesForMedication(long medicationId, OnResultListener<List<Schedule>> listener) {
        dbClient.executor().execute(() -> {
            List<Schedule> schedules = dbClient.db().scheduleDao().getSchedulesByMedication(medicationId);
            mainHandler.post(() -> listener.onResult(schedules));
        });
    }

    public void getAllSchedules(OnResultListener<List<Schedule>> listener) {
        dbClient.executor().execute(() -> {
            List<Schedule> schedules = dbClient.db().scheduleDao().getAllSchedules();
            mainHandler.post(() -> listener.onResult(schedules));
        });
    }

    public void insertMedicationWithSchedule(Medication medication, Schedule schedule, OnResultListener<Long> listener) {
        dbClient.executor().execute(() -> {
            dbClient.db().runInTransaction(() -> {
                long medId = dbClient.db().medicationDao().insert(medication);
                schedule.setMedicationId(medId);
                dbClient.db().scheduleDao().insert(schedule);
                com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
                mainHandler.post(() -> listener.onResult(medId));
            });
        });
    }

    public void updateMedicationWithSchedule(Medication medication, Schedule schedule, Runnable onComplete) {
        dbClient.executor().execute(() -> {
            dbClient.db().runInTransaction(() -> {
                dbClient.db().medicationDao().update(medication);
                dbClient.db().scheduleDao().update(schedule);
                com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            });
        });
    }

    public void deleteMedication(Medication medication, Runnable onComplete) {
        dbClient.executor().execute(() -> {
            dbClient.db().medicationDao().delete(medication);
            com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void insertIntakeLog(IntakeLog log, boolean force, OnLogResultListener listener) {
        dbClient.executor().execute(() -> {
            try {
                long logId;
                if (force) {
                    logId = dbClient.db().intakeLogDao().insertWithOverdoseForce(log);
                } else {
                    logId = dbClient.db().intakeLogDao().insertWithOverdoseGuard(log);
                }
                
                // Adjust current stock when a dose is taken
                if ("TAKEN".equals(log.getStatus())) {
                    Medication medication = dbClient.db().medicationDao().getMedicationById(log.getMedicationId());
                    if (medication != null && medication.getCurrentStock() != null) {
                        int currentStock = medication.getCurrentStock();
                        double quantity = medication.getDosageQuantity();
                        int newStock = Math.max(0, currentStock - (int) Math.ceil(quantity));
                        medication.setCurrentStock(newStock);
                        dbClient.db().medicationDao().update(medication);
                    }
                }

                com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
                mainHandler.post(() -> listener.onSuccess(logId));
            } catch (OverdoseException oe) {
                mainHandler.post(() -> listener.onOverdoseWarning(log, oe.getMessage()));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(e));
            }
        });
    }

    public void getLogsFiltered(long profileId, long startMillis, long endMillis, OnResultListener<List<IntakeLog>> listener) {
        dbClient.executor().execute(() -> {
            List<IntakeLog> logs = dbClient.db().intakeLogDao().getLogsFiltered(profileId, startMillis, endMillis);
            mainHandler.post(() -> listener.onResult(logs));
        });
    }

    public void getMedicationById(long medId, OnResultListener<Medication> listener) {
        dbClient.executor().execute(() -> {
            Medication med = dbClient.db().medicationDao().getMedicationById(medId);
            mainHandler.post(() -> listener.onResult(med));
        });
    }

    public void undoIntakeLog(long logId, long medicationId, Runnable onComplete) {
        dbClient.executor().execute(() -> {
            dbClient.db().runInTransaction(() -> {
                // 1. Restore stock
                Medication medication = dbClient.db().medicationDao().getMedicationById(medicationId);
                if (medication != null && medication.getCurrentStock() != null) {
                    int currentStock = medication.getCurrentStock();
                    double quantity = medication.getDosageQuantity();
                    medication.setCurrentStock(currentStock + (int) Math.ceil(quantity));
                    dbClient.db().medicationDao().update(medication);
                }
                // 2. Delete the log entry
                dbClient.db().intakeLogDao().deleteById(logId);
                com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
                
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            });
        });
    }
}
