package com.medpills.app.utils;

import com.medpills.app.database.Schedule;
import org.junit.Test;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.*;

public class ScheduleCalculatorTest {

    @Test
    public void testDailySchedule() {
        Schedule schedule = new Schedule();
        schedule.setFrequencyType("DAILY");
        schedule.setTargetTimes("08:00,20:00");

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(2026, Calendar.JUNE, 5, 12, 0, 0); // Target: June 5, 2026
        Date targetDate = targetCal.getTime();

        List<Long> times = ScheduleCalculator.calculateScheduledTimesForDay(schedule, targetDate);

        assertEquals(2, times.size());

        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(times.get(0));
        assertEquals(2026, c1.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, c1.get(Calendar.MONTH));
        assertEquals(5, c1.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, c1.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c1.get(Calendar.MINUTE));

        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(times.get(1));
        assertEquals(2026, c2.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, c2.get(Calendar.MONTH));
        assertEquals(5, c2.get(Calendar.DAY_OF_MONTH));
        assertEquals(20, c2.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c2.get(Calendar.MINUTE));
    }

    @Test
    public void testIntervalSchedule() {
        Schedule schedule = new Schedule();
        schedule.setFrequencyType("INTERVAL");
        schedule.setIntervalHours(8);

        // Start: June 5, 2026, at 04:00 AM
        Calendar startCal = Calendar.getInstance();
        startCal.set(2026, Calendar.JUNE, 5, 4, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        schedule.setStartDateMillis(startCal.getTimeInMillis());

        // Target Date: June 5, 2026
        Calendar targetCal = Calendar.getInstance();
        targetCal.set(2026, Calendar.JUNE, 5, 12, 0, 0);
        Date targetDate = targetCal.getTime();

        List<Long> times = ScheduleCalculator.calculateScheduledTimesForDay(schedule, targetDate);

        // Expect: 04:00, 12:00, 20:00 (3 times)
        assertEquals(3, times.size());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(times.get(0));
        assertEquals(4, cal.get(Calendar.HOUR_OF_DAY));

        cal.setTimeInMillis(times.get(1));
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));

        cal.setTimeInMillis(times.get(2));
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testSpecificDaysSchedule() {
        Schedule schedule = new Schedule();
        schedule.setFrequencyType("SPECIFIC_DAYS");
        schedule.setDaysOfWeek("MONDAY,WEDNESDAY,FRIDAY");
        schedule.setTargetTimes("09:00");

        // Target: June 5, 2026 (which is a Friday)
        Calendar targetCal = Calendar.getInstance();
        targetCal.set(2026, Calendar.JUNE, 5, 12, 0, 0);
        Date targetDate = targetCal.getTime();

        List<Long> times = ScheduleCalculator.calculateScheduledTimesForDay(schedule, targetDate);
        assertEquals(1, times.size());

        // Target: June 6, 2026 (which is a Saturday)
        targetCal.set(2026, Calendar.JUNE, 6, 12, 0, 0);
        Date targetDateSat = targetCal.getTime();

        List<Long> timesSat = ScheduleCalculator.calculateScheduledTimesForDay(schedule, targetDateSat);
        assertTrue(timesSat.isEmpty());
    }

    @Test
    public void testCyclicSchedule() {
        Schedule schedule = new Schedule();
        schedule.setFrequencyType("CYCLIC");
        schedule.setCycleOnDays(3);
        schedule.setCycleOffDays(2);
        schedule.setTargetTimes("10:00");

        // Start: June 1, 2026 (Day 1 - ON)
        Calendar startCal = Calendar.getInstance();
        startCal.set(2026, Calendar.JUNE, 1, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        schedule.setStartDateMillis(startCal.getTimeInMillis());

        // Target: June 3, 2026 (Day 3 - ON) -> Should generate a time
        Calendar targetCal = Calendar.getInstance();
        targetCal.set(2026, Calendar.JUNE, 3, 12, 0, 0);
        List<Long> timesDay3 = ScheduleCalculator.calculateScheduledTimesForDay(schedule, targetCal.getTime());
        assertEquals(1, timesDay3.size());

        // Target: June 4, 2026 (Day 4 - OFF) -> Should be empty
        targetCal.set(2026, Calendar.JUNE, 4, 12, 0, 0);
        List<Long> timesDay4 = ScheduleCalculator.calculateScheduledTimesForDay(schedule, targetCal.getTime());
        assertTrue(timesDay4.isEmpty());
    }
}
