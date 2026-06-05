package com.medpills.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import com.medpills.R;
import com.medpills.database.DatabaseClient;
import com.medpills.database.IntakeLog;
import com.medpills.database.Medication;

public class TodayIntakesWidget extends AppWidgetProvider {

    public static final String ACTION_CHECK = "com.medpills.widget.ACTION_CHECK";
    public static final String EXTRA_MED_ID = "com.medpills.widget.EXTRA_MED_ID";
    public static final String EXTRA_SCHED_TIME = "com.medpills.widget.EXTRA_SCHED_TIME";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today_intakes);

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

        appWidgetManager.updateAppWidget(appWidgetId, views);
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
