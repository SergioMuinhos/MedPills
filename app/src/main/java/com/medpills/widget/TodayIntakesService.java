package com.medpills.widget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.medpills.R;
import com.medpills.database.DatabaseClient;
import com.medpills.database.IntakeLog;
import com.medpills.database.Medication;
import com.medpills.database.Schedule;
import com.medpills.utils.DateTimeUtils;
import com.medpills.utils.ScheduleCalculator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodayIntakesService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodayIntakesFactory(this.getApplicationContext());
    }

    static class TodayIntakesFactory implements RemoteViewsFactory {
        private final Context context;
        private final List<WidgetIntakeItem> items = new ArrayList<>();
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        TodayIntakesFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {}

        @Override
        public void onDataSetChanged() {
            Log.d("TodayIntakesFactory", "onDataSetChanged triggered");
            items.clear();
            try {
                Date now = new Date();
                long start = DateTimeUtils.getStartOfDay(now);
                long end = DateTimeUtils.getEndOfDay(now);

                Log.d("TodayIntakesFactory", "Filtering from " + start + " to " + end);

                // Efficiently fetch all data
                List<Medication> medications = DatabaseClient.getInstance(context).db().medicationDao().getAllMedications();
                List<Schedule> allSchedules = DatabaseClient.getInstance(context).db().scheduleDao().getAllSchedules();
                List<IntakeLog> logs = DatabaseClient.getInstance(context).db().intakeLogDao().getLogsFilteredAcrossProfiles(start, end);

                Log.d("TodayIntakesFactory", "Meds: " + medications.size() + ", Scheds: " + allSchedules.size() + ", Logs: " + logs.size());

                Map<Long, Medication> medMap = new HashMap<>();
                for (Medication med : medications) {
                    medMap.put(med.getId(), med);
                }

                for (Schedule sched : allSchedules) {
                    if (sched.getEndDateMillis() != null && sched.getEndDateMillis() < start) continue;
                    
                    Medication med = medMap.get(sched.getMedicationId());
                    if (med == null) continue;

                    List<Long> scheduledTimes = ScheduleCalculator.calculateScheduledTimesForDay(sched, now);
                    for (long schedTime : scheduledTimes) {
                        String status = getLogStatus(logs, med.getId(), schedTime);
                        items.add(new WidgetIntakeItem(med.getId(), med.getName(), schedTime, status));
                    }
                }
                items.sort((a, b) -> Long.compare(a.time, b.time));
                Log.d("TodayIntakesFactory", "Items loaded: " + items.size());
            } catch (Exception e) {
                Log.e("TodayIntakesFactory", "Error loading widget data", e);
            }
        }

        private String getLogStatus(List<IntakeLog> logs, long medId, long schedTime) {
            for (IntakeLog log : logs) {
                if (log.getMedicationId() == medId && log.getScheduledTimeMillis() == schedTime) {
                    return log.getStatus();
                }
            }
            return "PENDING";
        }

        @Override
        public void onDestroy() {
            items.clear();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= items.size()) return null;
            
            WidgetIntakeItem item = items.get(position);
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item_intake);
            rv.setTextViewText(R.id.widget_med_time, timeFormat.format(new Date(item.time)));

            String status = item.status;
            if ("TAKEN".equals(status)) {
                // If taken, hide click button and other indicators, just show dimmed text
                rv.setViewVisibility(R.id.btn_widget_check, android.view.View.GONE);
                rv.setViewVisibility(R.id.iv_widget_checked, android.view.View.GONE);
                rv.setViewVisibility(R.id.iv_widget_skipped, android.view.View.GONE);
                
                rv.setTextViewText(R.id.widget_med_name, item.name + " (Tomado)");
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.text_secondary));
            } else if ("SKIPPED".equals(status)) {
                // If skipped, hide click button and other indicators, just show dimmed text
                rv.setViewVisibility(R.id.btn_widget_check, android.view.View.GONE);
                rv.setViewVisibility(R.id.iv_widget_checked, android.view.View.GONE);
                rv.setViewVisibility(R.id.iv_widget_skipped, android.view.View.GONE);
                
                rv.setTextViewText(R.id.widget_med_name, item.name + " (Omitido)");
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.text_secondary));
            } else if ("POSTPONED".equals(status)) {
                // If postponed, keep check button active, show warning color
                rv.setViewVisibility(R.id.btn_widget_check, android.view.View.VISIBLE);
                rv.setViewVisibility(R.id.iv_widget_checked, android.view.View.GONE);
                rv.setViewVisibility(R.id.iv_widget_skipped, android.view.View.GONE);
                
                rv.setTextViewText(R.id.widget_med_name, item.name + " (Pospuesto)");
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.warning));
                
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(TodayIntakesWidget.EXTRA_MED_ID, item.id);
                fillInIntent.putExtra(TodayIntakesWidget.EXTRA_SCHED_TIME, item.time);
                rv.setOnClickFillInIntent(R.id.btn_widget_check, fillInIntent);
            } else {
                // If pending, show click button, hide checked/skipped indicators, normal text color
                rv.setViewVisibility(R.id.btn_widget_check, android.view.View.VISIBLE);
                rv.setViewVisibility(R.id.iv_widget_checked, android.view.View.GONE);
                rv.setViewVisibility(R.id.iv_widget_skipped, android.view.View.GONE);
                
                rv.setTextViewText(R.id.widget_med_name, item.name);
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.text_primary));
                
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(TodayIntakesWidget.EXTRA_MED_ID, item.id);
                fillInIntent.putExtra(TodayIntakesWidget.EXTRA_SCHED_TIME, item.time);
                rv.setOnClickFillInIntent(R.id.btn_widget_check, fillInIntent);
            }

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(context.getPackageName(), R.layout.widget_item_intake);
        }

        @Override
        public int getViewTypeCount() { return 1; }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public boolean hasStableIds() { return true; }
    }

    private static class WidgetIntakeItem {
        long id;
        String name;
        long time;
        String status;
        WidgetIntakeItem(long id, String name, long time, String status) {
            this.id = id;
            this.name = name;
            this.time = time;
            this.status = status;
        }
    }
}
