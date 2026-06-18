package com.medpills.alarm;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.medpills.database.AppDatabase;
import com.medpills.database.DatabaseClient;
import com.medpills.database.IntakeLog;
import com.medpills.database.Medication;
import com.medpills.database.Schedule;
import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    public static final String ACTION_MARK_TAKEN = "com.medpills.ACTION_MARK_TAKEN";
    public static final String ACTION_POSTPONE = "com.medpills.ACTION_POSTPONE";
    public static final String ACTION_REFRESH_WIDGET = "com.medpills.ACTION_REFRESH_WIDGET";
    public static final String CHANNEL_ID = "medpills_notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive triggered with action: " + action);

        final PendingResult pendingResult = goAsync();
        DatabaseClient.getInstance(context).executor().execute(() -> {
            try {
                if (ACTION_MARK_TAKEN.equals(action)) {
                    handleMarkAsTaken(context, intent);
                } else if (ACTION_POSTPONE.equals(action)) {
                    handlePostpone(context, intent);
                } else if (ACTION_REFRESH_WIDGET.equals(action)) {
                    handleRefreshWidget(context);
                } else {
                    handleAlarmTrigger(context, intent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error executing receiver background task", e);
            } finally {
                pendingResult.finish();
                Log.d(TAG, "goAsync pendingResult finished.");
            }
        });
    }

    private void handleAlarmTrigger(Context context, Intent intent) {
        long scheduleId = intent.getLongExtra("schedule_id", -1);
        long medicationId = intent.getLongExtra("medication_id", -1);
        String medName = intent.getStringExtra("medication_name");
        double quantity = intent.getDoubleExtra("dosage_quantity", 1.0);
        String unit = intent.getStringExtra("dosage_unit");
        long profileId = intent.getLongExtra("profile_id", -1);
        long scheduledTime = intent.getLongExtra("scheduled_time_millis", System.currentTimeMillis());

        if (scheduleId == -1 || medicationId == -1) {
            Log.e(TAG, "Invalid parameters inside alarm trigger intent");
            return;
        }

        Log.d(TAG, "Displaying reminder for medication: " + medName);
        showNotification(context, scheduleId, medicationId, medName, quantity, unit, profileId, scheduledTime);

        // Schedule next alarm for this schedule (so it automatically queues for the future)
        AppDatabase db = DatabaseClient.getInstance(context).db();
        Schedule schedule = db.scheduleDao().getSchedulesByMedication(medicationId).stream()
                .filter(s -> s.getId() == scheduleId)
                .findFirst().orElse(null);
        if (schedule != null) {
            AlarmHelper.scheduleNextAlarm(context, schedule);
        }
    }

    private void handleMarkAsTaken(Context context, Intent intent) {
        long scheduleId = intent.getLongExtra("schedule_id", -1);
        long medicationId = intent.getLongExtra("medication_id", -1);
        long profileId = intent.getLongExtra("profile_id", -1);
        long scheduledTime = intent.getLongExtra("scheduled_time_millis", System.currentTimeMillis());

        if (medicationId == -1 || profileId == -1) return;

        AppDatabase db = DatabaseClient.getInstance(context).db();

        // Check if stock exists and decrement it
        Medication medication = db.medicationDao().getMedicationById(medicationId);
        if (medication != null && medication.getCurrentStock() != null) {
            int currentStock = medication.getCurrentStock();
            double quantity = medication.getDosageQuantity();
            int newStock = Math.max(0, currentStock - (int) Math.ceil(quantity));
            medication.setCurrentStock(newStock);
            db.medicationDao().update(medication);
        }

        // Insert log: status = TAKEN
        IntakeLog log = new IntakeLog(medicationId, profileId, System.currentTimeMillis(), scheduledTime, "TAKEN");
        db.intakeLogDao().insert(log);
        com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);

        Log.d(TAG, "Medication ID " + medicationId + " marked as taken via notification action.");

        // Cancel the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel((int) scheduleId);
        }
    }

    private void handlePostpone(Context context, Intent intent) {
        long scheduleId = intent.getLongExtra("schedule_id", -1);
        long medicationId = intent.getLongExtra("medication_id", -1);
        String medName = intent.getStringExtra("medication_name");
        double quantity = intent.getDoubleExtra("dosage_quantity", 1.0);
        String unit = intent.getStringExtra("dosage_unit");
        long profileId = intent.getLongExtra("profile_id", -1);

        if (scheduleId == -1 || medicationId == -1) return;

        long postponeTime = System.currentTimeMillis() + (15 * 60 * 1000); // 15 minutes later

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent postponeIntent = new Intent(context, AlarmReceiver.class);
            postponeIntent.setAction("com.medpills.ALARM_TRIGGER");
            postponeIntent.putExtra("schedule_id", scheduleId);
            postponeIntent.putExtra("medication_id", medicationId);
            postponeIntent.putExtra("medication_name", medName);
            postponeIntent.putExtra("dosage_quantity", quantity);
            postponeIntent.putExtra("dosage_unit", unit);
            postponeIntent.putExtra("profile_id", profileId);
            postponeIntent.putExtra("scheduled_time_millis", postponeTime);

            // Use request code offset to avoid overriding standard schedules
            int requestCode = (int) scheduleId + 100000;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    postponeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        postponeTime,
                        pendingIntent
                );
                Log.d(TAG, "Postponed alarm scheduled for 15m later: " + postponeTime);
            } catch (SecurityException e) {
                Log.e(TAG, "Could not schedule postponed alarm", e);
            }
        }

        // Cancel the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel((int) scheduleId);
        }
    }

    private void handleRefreshWidget(Context context) {
        Log.d(TAG, "Refreshing widget from midnight alarm.");
        com.medpills.widget.TodayIntakesWidget.notifyWidgetDataChanged(context);
        // Reschedule for tomorrow
        AlarmHelper.scheduleMidnightRefresh(context);
    }

    private void showNotification(Context context, long scheduleId, long medicationId, String medName,
                                  double quantity, String unit, long profileId, long scheduledTime) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Build notification channel
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Medicamentos",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Alarmas críticas para la toma de medicamentos");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 200, 500});
        
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (defaultSoundUri == null) {
            defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        channel.setSound(defaultSoundUri, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());

        notificationManager.createNotificationChannel(channel);

        // Action Intents
        Intent takenIntent = new Intent(context, AlarmReceiver.class);
        takenIntent.setAction(ACTION_MARK_TAKEN);
        takenIntent.putExtra("schedule_id", scheduleId);
        takenIntent.putExtra("medication_id", medicationId);
        takenIntent.putExtra("profile_id", profileId);
        takenIntent.putExtra("scheduled_time_millis", scheduledTime);
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) scheduleId,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent postponeIntent = new Intent(context, AlarmReceiver.class);
        postponeIntent.setAction(ACTION_POSTPONE);
        postponeIntent.putExtra("schedule_id", scheduleId);
        postponeIntent.putExtra("medication_id", medicationId);
        postponeIntent.putExtra("medication_name", medName);
        postponeIntent.putExtra("dosage_quantity", quantity);
        postponeIntent.putExtra("dosage_unit", unit);
        postponeIntent.putExtra("profile_id", profileId);
        PendingIntent postponePendingIntent = PendingIntent.getBroadcast(
                context,
                (int) scheduleId + 50000,
                postponeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Content Action (Launches App)
        Intent launchIntent = new Intent(context, com.medpills.ui.MainActivity.class);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                context,
                (int) scheduleId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Recordatorio de Medicamento")
                .setContentText("Es hora de tomar " + quantity + " " + unit + " de " + medName)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(launchPendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .addAction(android.R.drawable.checkbox_on_background, "Marcar como Tomado", takenPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "Posponer 15m", postponePendingIntent);

        notificationManager.notify((int) scheduleId, builder.build());
    }
}
