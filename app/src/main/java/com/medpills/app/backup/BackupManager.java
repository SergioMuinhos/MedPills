package com.medpills.app.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.medpills.app.alarm.AlarmHelper;
import com.medpills.app.database.AppDatabase;
import com.medpills.app.database.DatabaseClient;
import com.medpills.app.database.IntakeLog;
import com.medpills.app.database.Medication;
import com.medpills.app.database.Profile;
import com.medpills.app.database.Schedule;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BackupManager {
    private static final String TAG = "BackupManager";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnBackupListener {
        void onSuccess();
        void onError(Exception e);
    }

    public static void exportData(Context context, Uri fileUri, OnBackupListener listener) {
        DatabaseClient.getInstance(context).executor().execute(() -> {
            try {
                AppDatabase db = DatabaseClient.getInstance(context).db();
                
                // Read all data
                List<Profile> profiles = db.profileDao().getAllProfiles();
                List<Medication> medications = db.medicationDao().getAllMedications();
                List<Schedule> schedules = db.scheduleDao().getAllSchedules();
                List<IntakeLog> logs = db.intakeLogDao().getAllLogs();

                BackupPayload payload = new BackupPayload();
                payload.profiles = profiles;
                payload.medications = medications;
                payload.schedules = schedules;
                payload.intakeLogs = logs;

                Gson gson = new Gson();
                String jsonStr = gson.toJson(payload);

                try (OutputStream os = context.getContentResolver().openOutputStream(fileUri);
                     OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                    osw.write(jsonStr);
                    osw.flush();
                }

                mainHandler.post(listener::onSuccess);
                Log.d(TAG, "Database exported successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Error exporting data", e);
                mainHandler.post(() -> listener.onError(e));
            }
        });
    }

    public static void importData(Context context, Uri fileUri, OnBackupListener listener) {
        DatabaseClient.getInstance(context).executor().execute(() -> {
            try {
                // 1. Read JSON input stream completely into memory
                StringBuilder sb = new StringBuilder();
                try (InputStream is = context.getContentResolver().openInputStream(fileUri);
                     BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }

                String jsonStr = sb.toString();

                // 2. Parse and validate structure using GSON
                Gson gson = new Gson();
                BackupPayload payload;
                try {
                    payload = gson.fromJson(jsonStr, BackupPayload.class);
                } catch (JsonSyntaxException jse) {
                    Log.e(TAG, "JSON Syntax Error during parsing", jse);
                    mainHandler.post(() -> listener.onError(new IllegalArgumentException("Formato JSON inválido", jse)));
                    return;
                }

                if (payload == null || payload.profiles == null) {
                    Log.e(TAG, "JSON structural validation failed: Null profiles detected.");
                    mainHandler.post(() -> listener.onError(new IllegalArgumentException("El archivo de copia de seguridad no contiene la estructura requerida")));
                    return;
                }

                if (payload.medications == null) {
                    payload.medications = new java.util.ArrayList<>();
                }
                if (payload.schedules == null) {
                    payload.schedules = new java.util.ArrayList<>();
                }
                if (payload.intakeLogs == null) {
                    payload.intakeLogs = new java.util.ArrayList<>();
                }

                // 3. Perform database operations inside a Room database transaction
                AppDatabase db = DatabaseClient.getInstance(context).db();
                db.runInTransaction(() -> {
                    // Purge old data completely
                    // Foreign keys ON DELETE CASCADE will handle cascading deletes, but manual purge of all tables is safest
                    // We must delete from children first to respect foreign keys if delete cascade is not relied upon
                    // However, Room transactions allow disabling foreign keys momentarily or clearing in order:
                    // Order of purge: logs -> schedules -> medications -> profiles
                    
                    // Simple query-based purges:
                    db.query("DELETE FROM intake_logs", null).close();
                    db.query("DELETE FROM schedules", null).close();
                    db.query("DELETE FROM medications", null).close();
                    db.query("DELETE FROM profiles", null).close();

                    // Insert profiles
                    for (Profile profile : payload.profiles) {
                        db.profileDao().insert(profile);
                    }

                    // Insert medications
                    for (Medication med : payload.medications) {
                        db.medicationDao().insert(med);
                    }

                    // Insert schedules
                    for (Schedule sched : payload.schedules) {
                        db.scheduleDao().insert(sched);
                    }

                    // Insert intake logs
                    for (IntakeLog log : payload.intakeLogs) {
                        db.intakeLogDao().insert(log);
                    }
                });

                Log.d(TAG, "Import completed. Triggering alarm rescheduling.");
                // 4. Instantly trigger Alarm Engine to recalculate and reprogram alarms
                AlarmHelper.rescheduleAllAlarms(context);
                com.medpills.app.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);

                mainHandler.post(listener::onSuccess);
            } catch (Exception e) {
                Log.e(TAG, "Error importing data", e);
                mainHandler.post(() -> listener.onError(e));
            }
        });
    }
}
