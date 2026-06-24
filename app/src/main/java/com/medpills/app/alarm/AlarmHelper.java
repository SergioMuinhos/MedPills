package com.medpills.app.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.medpills.app.database.DatabaseClient;
import com.medpills.app.database.Medication;
import com.medpills.app.database.Schedule;
import java.util.Calendar;
import java.util.List;

public class AlarmHelper {
    private static final String TAG = "AlarmHelper";

    public static void rescheduleAllAlarms(Context context) {
        Log.d(TAG, "Rescheduling all alarms.");
        scheduleMidnightRefresh(context);
        DatabaseClient.getInstance(context).executor().execute(() -> {
            List<Schedule> schedules = DatabaseClient.getInstance(context).db().scheduleDao().getAllSchedules();
            for (Schedule schedule : schedules) {
                scheduleNextAlarm(context, schedule);
            }
        });
    }

    public static void scheduleMidnightRefresh(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || !alarmManager.canScheduleExactAlarms()) return;

        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 1); // 1 second past midnight to be sure
        midnight.set(Calendar.MILLISECOND, 0);
        
        if (midnight.getTimeInMillis() <= System.currentTimeMillis()) {
            midnight.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_REFRESH_WIDGET);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                999999, // Unique request code for refresh
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    midnight.getTimeInMillis(),
                    pendingIntent
            );
            Log.d(TAG, "Midnight refresh scheduled at: " + midnight.getTime().toString());
        } catch (SecurityException e) {
            Log.e(TAG, "Could not schedule midnight refresh", e);
        }
    }

    public static void scheduleNextAlarm(Context context, Schedule schedule) {
        scheduleNextAlarmAfter(context, schedule, System.currentTimeMillis());
    }

    public static void scheduleNextAlarmAfter(Context context, Schedule schedule, long afterMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Check if we can schedule exact alarms
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms - permission not granted.");
            return;
        }

        long nextTriggerMillis = calculateNextTriggerTime(schedule, afterMillis);
        if (nextTriggerMillis <= 0) {
            Log.d(TAG, "No upcoming alarms for schedule: " + schedule.getId());
            return;
        }

        // Retrieve medication details
        DatabaseClient.getInstance(context).executor().execute(() -> {
            Medication medication = DatabaseClient.getInstance(context).db().medicationDao().getMedicationById(schedule.getMedicationId());
            if (medication == null) {
                Log.e(TAG, "Medication not found for schedule: " + schedule.getId());
                return;
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("com.medpills.app.ALARM_TRIGGER");
            intent.putExtra("schedule_id", schedule.getId());
            intent.putExtra("medication_id", medication.getId());
            intent.putExtra("medication_name", medication.getName());
            intent.putExtra("dosage_quantity", medication.getDosageQuantity());
            intent.putExtra("dosage_unit", medication.getDosageUnit());
            intent.putExtra("profile_id", medication.getProfileId());
            intent.putExtra("scheduled_time_millis", nextTriggerMillis);

            int requestCode = (int) schedule.getId();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule the exact alarm bypassing Doze Mode
            try {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled alarm for " + medication.getName() + " (ID: " + medication.getId() + ") at " + nextTriggerMillis);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException scheduling exact alarm", e);
            }
        });
    }

    public static void cancelAlarm(Context context, Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.medpills.app.ALARM_TRIGGER");
        int requestCode = (int) schedule.getId();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm for schedule ID: " + schedule.getId());
        }
    }

    public static long calculateNextTriggerTime(Schedule schedule, long fromMillis) {
        String type = schedule.getFrequencyType();
        if ("DAILY".equals(type)) {
            return getNextDailyTime(schedule.getTargetTimes(), fromMillis);
        } else if ("INTERVAL".equals(type)) {
            long start = schedule.getStartDateMillis();
            long intervalMs = schedule.getIntervalHours() * 3600 * 1000L;
            if (intervalMs <= 0) return 0;

            if (start > fromMillis) {
                return start;
            } else {
                long diff = fromMillis - start;
                long count = (diff / intervalMs) + 1;
                return start + (count * intervalMs);
            }
        } else if ("SPECIFIC_DAYS".equals(type)) {
            return getNextSpecificDaysTime(schedule.getDaysOfWeek(), schedule.getTargetTimes(), fromMillis);
        } else if ("CYCLIC".equals(type)) {
            return getNextCyclicTime(schedule, fromMillis);
        }
        return 0;
    }

    private static long getNextDailyTime(String targetTimesStr, long fromMillis) {
        if (targetTimesStr == null || targetTimesStr.isEmpty()) return 0;
        String[] times = targetTimesStr.split(",");
        long bestTime = Long.MAX_VALUE;

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(fromMillis);

        for (String timeStr : times) {
            String[] parts = timeStr.trim().split(":");
            if (parts.length != 2) continue;
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(fromMillis);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            // If the time has already passed today, try tomorrow
            if (cal.getTimeInMillis() <= fromMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            long time = cal.getTimeInMillis();
            if (time < bestTime) {
                bestTime = time;
            }
        }

        return bestTime == Long.MAX_VALUE ? 0 : bestTime;
    }

    private static long getNextSpecificDaysTime(String daysOfWeekStr, String targetTimesStr, long fromMillis) {
        if (daysOfWeekStr == null || daysOfWeekStr.isEmpty() || targetTimesStr == null || targetTimesStr.isEmpty()) return 0;
        
        long bestTime = Long.MAX_VALUE;
        // Search next 7 days
        for (int dayOffset = 0; dayOffset < 8; dayOffset++) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(fromMillis);
            cal.add(Calendar.DAY_OF_YEAR, dayOffset);
            
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String dayName = getDayName(dayOfWeek);

            if (daysOfWeekStr.toUpperCase().contains(dayName)) {
                String[] times = targetTimesStr.split(",");
                for (String timeStr : times) {
                    String[] parts = timeStr.trim().split(":");
                    if (parts.length != 2) continue;
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);

                    Calendar timeCal = (Calendar) cal.clone();
                    timeCal.set(Calendar.HOUR_OF_DAY, hour);
                    timeCal.set(Calendar.MINUTE, minute);
                    timeCal.set(Calendar.SECOND, 0);
                    timeCal.set(Calendar.MILLISECOND, 0);

                    long time = timeCal.getTimeInMillis();
                    if (time > fromMillis && time < bestTime) {
                        bestTime = time;
                    }
                }
            }
        }
        return bestTime == Long.MAX_VALUE ? 0 : bestTime;
    }

    private static long getNextCyclicTime(Schedule schedule, long fromMillis) {
        long startDate = schedule.getStartDateMillis();
        int onDays = schedule.getCycleOnDays();
        int offDays = schedule.getCycleOffDays();
        if (onDays <= 0) return 0;
        
        long bestTime = Long.MAX_VALUE;
        // Check next 60 days to find an active day
        for (int dayOffset = 0; dayOffset < 60; dayOffset++) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(fromMillis);
            cal.add(Calendar.DAY_OF_YEAR, dayOffset);
            
            // Set calendar to start of day
            Calendar startCal = (Calendar) cal.clone();
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            
            Calendar startScheduleCal = Calendar.getInstance();
            startScheduleCal.setTimeInMillis(startDate);
            startScheduleCal.set(Calendar.HOUR_OF_DAY, 0);
            startScheduleCal.set(Calendar.MINUTE, 0);
            startScheduleCal.set(Calendar.SECOND, 0);
            startScheduleCal.set(Calendar.MILLISECOND, 0);
            
            long diffMillis = startCal.getTimeInMillis() - startScheduleCal.getTimeInMillis();
            if (diffMillis < 0) continue; // Cycle hasn't started yet
            
            long diffDays = diffMillis / (24 * 3600 * 1000L);
            long cycleLength = onDays + offDays;
            long dayWithinCycle = diffDays % cycleLength;
            
            if (dayWithinCycle < onDays) {
                // It is an active day
                String[] times = schedule.getTargetTimes().split(",");
                for (String timeStr : times) {
                    String[] parts = timeStr.trim().split(":");
                    if (parts.length != 2) continue;
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);

                    Calendar timeCal = (Calendar) cal.clone();
                    timeCal.set(Calendar.HOUR_OF_DAY, hour);
                    timeCal.set(Calendar.MINUTE, minute);
                    timeCal.set(Calendar.SECOND, 0);
                    timeCal.set(Calendar.MILLISECOND, 0);

                    long time = timeCal.getTimeInMillis();
                    if (time > fromMillis && time < bestTime) {
                        bestTime = time;
                    }
                }
            }
        }
        return bestTime == Long.MAX_VALUE ? 0 : bestTime;
    }

    private static String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY: return "SUNDAY";
            case Calendar.MONDAY: return "MONDAY";
            case Calendar.TUESDAY: return "TUESDAY";
            case Calendar.WEDNESDAY: return "WEDNESDAY";
            case Calendar.THURSDAY: return "THURSDAY";
            case Calendar.FRIDAY: return "FRIDAY";
            case Calendar.SATURDAY: return "SATURDAY";
            default: return "";
        }
    }
}
