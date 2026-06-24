package com.medpills.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import com.medpills.app.R;
import com.medpills.app.database.DatabaseClient;
import com.medpills.app.database.IntakeLog;
import com.medpills.app.database.Medication;
import com.medpills.app.database.Schedule;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodayIntakesWidget extends AppWidgetProvider {

    public static final String ACTION_CHECK = "com.medpills.app.widget.ACTION_CHECK";
    public static final String EXTRA_MED_ID = "com.medpills.app.widget.EXTRA_MED_ID";
    public static final String EXTRA_SCHED_TIME = "com.medpills.app.widget.EXTRA_SCHED_TIME";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today_intakes);

        // Header click launches the main app
        Intent mainIntent = new Intent(context, com.medpills.app.ui.MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_header_root, mainPendingIntent);

        Intent intent = new Intent(context, TodayIntakesService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list, intent);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view);

        Intent clickIntent = new Intent(context, TodayIntakesWidget.class);
        clickIntent.setAction(ACTION_CHECK);
        clickIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent);

        // Fetch counts and update subtitle asynchronously
        DatabaseClient.getInstance(context).executor().execute(() -> {
            try {
                Date now = new Date();
                long start = com.medpills.app.utils.DateTimeUtils.getStartOfDay(now);
                long end = com.medpills.app.utils.DateTimeUtils.getEndOfDay(now);

                List<Medication> medications = DatabaseClient.getInstance(context).db().medicationDao().getAllMedications();
                List<Schedule> allSchedules = DatabaseClient.getInstance(context).db().scheduleDao().getAllSchedules();
                List<IntakeLog> logs = DatabaseClient.getInstance(context).db().intakeLogDao().getLogsFilteredAcrossProfiles(start, end);

                int totalCount = 0;
                int takenCount = 0;
                int pendingCount = 0;

                Map<Long, Medication> medMap = new HashMap<>();
                for (Medication med : medications) {
                    medMap.put(med.getId(), med);
                }

                for (Schedule sched : allSchedules) {
                    if (sched.getEndDateMillis() != null && sched.getEndDateMillis() < start) continue;
                    Medication med = medMap.get(sched.getMedicationId());
                    if (med == null) continue;

                    List<Long> scheduledTimes = com.medpills.app.utils.ScheduleCalculator.calculateScheduledTimesForDay(sched, now);
                    for (long schedTime : scheduledTimes) {
                        totalCount++;
                        boolean foundLog = false;
                        for (IntakeLog log : logs) {
                            if (log.getMedicationId() == med.getId() && log.getScheduledTimeMillis() == schedTime) {
                                foundLog = true;
                                if ("TAKEN".equals(log.getStatus())) {
                                    takenCount++;
                                }
                                break;
                            }
                        }
                        if (!foundLog) {
                            pendingCount++;
                        }
                    }
                }

                String subtitle;
                if (totalCount == 0) {
                    subtitle = "Sin tomas programadas";
                } else if (pendingCount == 0) {
                    subtitle = "¡Todo al día! (" + takenCount + "/" + totalCount + " tomados) 🎉";
                } else {
                    subtitle = pendingCount + " pendiente" + (pendingCount > 1 ? "s" : "") + " (" + takenCount + "/" + totalCount + " completados)";
                }

                views.setTextViewText(R.id.widget_subtitle, subtitle);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            } catch (Exception e) {
                android.util.Log.e("TodayIntakesWidget", "Error updating widget subtitle", e);
                // Fallback in case of database errors
                views.setTextViewText(R.id.widget_subtitle, "Tomas de hoy");
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        android.util.Log.d("TodayIntakesWidget", "onReceive Action: " + intent.getAction());
        if (ACTION_CHECK.equals(intent.getAction())) {
            long medId = intent.getLongExtra(EXTRA_MED_ID, -1);
            long schedTime = intent.getLongExtra(EXTRA_SCHED_TIME, -1);
            android.util.Log.d("TodayIntakesWidget", "ACTION_CHECK extras: medId=" + medId + ", schedTime=" + schedTime);

            if (medId != -1 && schedTime != -1) {
                DatabaseClient.getInstance(context).executor().execute(() -> {
                    try {
                        Medication medication = DatabaseClient.getInstance(context).db().medicationDao().getMedicationById(medId);
                        long profileId = (medication != null) ? medication.getProfileId() : 1;
                        
                        IntakeLog log = new IntakeLog(medId, profileId, System.currentTimeMillis(), schedTime, "TAKEN");
                        DatabaseClient.getInstance(context).db().intakeLogDao().insert(log);

                        if (medication != null && medication.getCurrentStock() != null) {
                            int currentStock = medication.getCurrentStock();
                            double quantity = medication.getDosageQuantity();
                            int newStock = Math.max(0, currentStock - (int) Math.ceil(quantity));
                            medication.setCurrentStock(newStock);
                            DatabaseClient.getInstance(context).db().medicationDao().update(medication);
                        }

                        notifyWidgetDataChanged(context);
                        android.util.Log.d("TodayIntakesWidget", "Logged intake successfully inside executor");
                    } catch (Exception e) {
                        android.util.Log.e("TodayIntakesWidget", "Error writing database intake log from widget", e);
                    }
                });
            } else {
                android.util.Log.e("TodayIntakesWidget", "Invalid parameters in ACTION_CHECK");
            }
        }
    }

    public static void notifyWidgetDataChanged(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TodayIntakesWidget.class));
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
    }
}
