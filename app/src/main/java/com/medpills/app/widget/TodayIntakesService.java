package com.medpills.app.widget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.medpills.app.R;
import com.medpills.app.database.DatabaseClient;
import com.medpills.app.database.IntakeLog;
import com.medpills.app.database.Medication;
import com.medpills.app.database.Schedule;
import com.medpills.app.utils.DateTimeUtils;
import com.medpills.app.utils.ScheduleCalculator;
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

                // Fetch all data from database
                List<Medication> medications = DatabaseClient.getInstance(context).db().medicationDao().getAllMedications();
                List<Schedule> allSchedules = DatabaseClient.getInstance(context).db().scheduleDao().getAllSchedules();
                List<IntakeLog> logs = DatabaseClient.getInstance(context).db().intakeLogDao().getLogsFilteredAcrossProfiles(start, end);
                List<com.medpills.app.database.Profile> profiles = DatabaseClient.getInstance(context).db().profileDao().getAllProfiles();

                Log.d("TodayIntakesFactory", "Meds: " + medications.size() + ", Scheds: " + allSchedules.size() + ", Logs: " + logs.size());

                Map<Long, Medication> medMap = new HashMap<>();
                for (Medication med : medications) {
                    medMap.put(med.getId(), med);
                }

                Map<Long, String> profileMap = new HashMap<>();
                for (com.medpills.app.database.Profile profile : profiles) {
                    profileMap.put(profile.getId(), profile.getName());
                }

                for (Schedule sched : allSchedules) {
                    if (sched.getEndDateMillis() != null && sched.getEndDateMillis() < start) continue;
                    
                    Medication med = medMap.get(sched.getMedicationId());
                    if (med == null) continue;

                    List<Long> scheduledTimes = ScheduleCalculator.calculateScheduledTimesForDay(sched, now);
                    for (long schedTime : scheduledTimes) {
                        String status = getLogStatus(logs, med.getId(), schedTime);
                        String profileName = profileMap.getOrDefault(med.getProfileId(), "Principal");
                        items.add(new WidgetIntakeItem(
                                med.getId(), 
                                med.getName(), 
                                schedTime, 
                                status,
                                med.getDosageQuantity(),
                                med.getDosageUnit(),
                                profileName
                        ));
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
            
            // Format time, dosage, and profile info
            String timeStr = timeFormat.format(new Date(item.time));
            String dosageStr = (item.dosageQuantity % 1 == 0) 
                    ? String.valueOf((int) item.dosageQuantity) 
                    : String.valueOf(item.dosageQuantity);
            String desc = timeStr + "  •  " + dosageStr + " " + item.dosageUnit + "  •  " + item.profileName;
            rv.setTextViewText(R.id.widget_med_desc, desc);

            String status = item.status;
            if ("TAKEN".equals(status)) {
                // Apply TAKEN status style
                rv.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_taken);
                rv.setInt(R.id.widget_item_indicator, "setBackgroundResource", R.drawable.widget_indicator_taken);
                
                rv.setTextViewText(R.id.widget_med_name, item.name + " (Tomado)");
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.text_secondary));
                rv.setTextColor(R.id.widget_med_desc, context.getColor(R.color.text_secondary));
                
                rv.setImageViewResource(R.id.btn_widget_check, R.drawable.ic_check_circle);
                rv.setInt(R.id.btn_widget_check, "setColorFilter", context.getColor(R.color.success));
                rv.setOnClickFillInIntent(R.id.btn_widget_check, null); // Clear click intent
            } else if ("SKIPPED".equals(status)) {
                // Apply SKIPPED status style
                rv.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_skipped);
                rv.setInt(R.id.widget_item_indicator, "setBackgroundResource", R.drawable.widget_indicator_skipped);
                
                rv.setTextViewText(R.id.widget_med_name, item.name + " (Omitido)");
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.text_secondary));
                rv.setTextColor(R.id.widget_med_desc, context.getColor(R.color.text_secondary));
                
                rv.setImageViewResource(R.id.btn_widget_check, R.drawable.ic_cancel_circle);
                rv.setInt(R.id.btn_widget_check, "setColorFilter", context.getColor(R.color.error));
                rv.setOnClickFillInIntent(R.id.btn_widget_check, null); // Clear click intent
            } else if ("POSTPONED".equals(status)) {
                // Apply POSTPONED status style
                rv.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_postponed);
                rv.setInt(R.id.widget_item_indicator, "setBackgroundResource", R.drawable.widget_indicator_postponed);
                
                rv.setTextViewText(R.id.widget_med_name, item.name + " (Pospuesto)");
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.warning));
                rv.setTextColor(R.id.widget_med_desc, context.getColor(R.color.text_secondary));
                
                rv.setImageViewResource(R.id.btn_widget_check, R.drawable.ic_clock_outline);
                rv.setInt(R.id.btn_widget_check, "setColorFilter", context.getColor(R.color.warning));
                
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(TodayIntakesWidget.EXTRA_MED_ID, item.id);
                fillInIntent.putExtra(TodayIntakesWidget.EXTRA_SCHED_TIME, item.time);
                rv.setOnClickFillInIntent(R.id.btn_widget_check, fillInIntent);
            } else {
                // Apply PENDING status style (default)
                rv.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_pending);
                rv.setInt(R.id.widget_item_indicator, "setBackgroundResource", R.drawable.widget_indicator_pending);
                
                rv.setTextViewText(R.id.widget_med_name, item.name);
                rv.setTextColor(R.id.widget_med_name, context.getColor(R.color.text_primary));
                rv.setTextColor(R.id.widget_med_desc, context.getColor(R.color.text_secondary));
                
                rv.setImageViewResource(R.id.btn_widget_check, R.drawable.ic_circle_outline);
                rv.setInt(R.id.btn_widget_check, "setColorFilter", context.getColor(R.color.primary));
                
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
        double dosageQuantity;
        String dosageUnit;
        String profileName;
        WidgetIntakeItem(long id, String name, long time, String status, double dosageQuantity, String dosageUnit, String profileName) {
            this.id = id;
            this.name = name;
            this.time = time;
            this.status = status;
            this.dosageQuantity = dosageQuantity;
            this.dosageUnit = dosageUnit;
            this.profileName = profileName;
        }
    }
}
