package com.medpills.app.utils;

import org.junit.Test;
import java.util.Calendar;
import java.util.Date;
import static org.junit.Assert.*;

public class DateTimeUtilsTest {

    @Test
    public void testGetStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JUNE, 5, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        Date testDate = cal.getTime();

        long startOfDay = DateTimeUtils.getStartOfDay(testDate);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(startOfDay);

        assertEquals(2026, resultCal.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, resultCal.get(Calendar.MONTH));
        assertEquals(5, resultCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, resultCal.get(Calendar.MINUTE));
        assertEquals(0, resultCal.get(Calendar.SECOND));
        assertEquals(0, resultCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testGetEndOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JUNE, 5, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        Date testDate = cal.getTime();

        long endOfDay = DateTimeUtils.getEndOfDay(testDate);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(endOfDay);

        assertEquals(2026, resultCal.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, resultCal.get(Calendar.MONTH));
        assertEquals(5, resultCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, resultCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, resultCal.get(Calendar.MINUTE));
        assertEquals(59, resultCal.get(Calendar.SECOND));
        assertEquals(999, resultCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testIsSameDay() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2026, Calendar.JUNE, 5, 10, 0, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2026, Calendar.JUNE, 5, 20, 0, 0);

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2026, Calendar.JUNE, 6, 10, 0, 0);

        assertTrue(DateTimeUtils.isSameDay(cal1.getTimeInMillis(), cal2.getTimeInMillis()));
        assertFalse(DateTimeUtils.isSameDay(cal1.getTimeInMillis(), cal3.getTimeInMillis()));
    }
}
