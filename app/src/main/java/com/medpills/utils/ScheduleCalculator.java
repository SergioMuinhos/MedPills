package com.medpills.utils;

import com.medpills.database.Schedule;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ScheduleCalculator {

    public static List<Long> calculateScheduledTimesForDay(Schedule schedule, Date targetDate) {
        List<Long> times = new ArrayList<>();
        long targetDateStart = DateTimeUtils.getStartOfDay(targetDate);
        long targetDateEnd = DateTimeUtils.getEndOfDay(targetDate);
        
        String type = schedule.getFrequencyType();
        if ("DAILY".equals(type)) {
            addTimesForDay(times, targetDateStart, schedule.getTargetTimes());
        } else if ("INTERVAL".equals(type)) {
            long start = schedule.getStartDateMillis();
            long intervalMs = schedule.getIntervalHours() * 3600 * 1000L;
            if (intervalMs > 0) {
                long current = start;
                while (current <= targetDateEnd) {
                    if (current >= targetDateStart && current <= targetDateEnd) {
                        times.add(current);
                    }
                    current += intervalMs;
                }
            }
        } else if ("SPECIFIC_DAYS".equals(type)) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(targetDate);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String dayName = getDayName(dayOfWeek);
            
            if (schedule.getDaysOfWeek() != null && schedule.getDaysOfWeek().toUpperCase().contains(dayName)) {
                addTimesForDay(times, targetDateStart, schedule.getTargetTimes());
            }
        } else if ("CYCLIC".equals(type)) {
            long startDate = schedule.getStartDateMillis();
            int onDays = schedule.getCycleOnDays();
            int offDays = schedule.getCycleOffDays();
            
            if (onDays > 0) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(startDate);
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);

                long diffMillis = targetDateStart - startCal.getTimeInMillis();
                if (diffMillis >= 0) {
                    long diffDays = diffMillis / (24 * 3600 * 1000L);
                    long cycleLength = onDays + offDays;
                    long dayWithinCycle = diffDays % cycleLength;
                    
                    if (dayWithinCycle < onDays) {
                        addTimesForDay(times, targetDateStart, schedule.getTargetTimes());
                    }
                }
            }
        }
        return times;
    }

    private static void addTimesForDay(List<Long> times, long dayStartMillis, String targetTimesStr) {
        if (targetTimesStr == null || targetTimesStr.isEmpty()) return;
        String[] timesArr = targetTimesStr.split(",");
        for (String timeStr : timesArr) {
            String[] parts = timeStr.trim().split(":");
            if (parts.length != 2) continue;
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(dayStartMillis);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            times.add(cal.getTimeInMillis());
        }
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
