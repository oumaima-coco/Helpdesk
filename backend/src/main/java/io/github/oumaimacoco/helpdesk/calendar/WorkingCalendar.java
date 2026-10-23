package io.github.oumaimacoco.helpdesk.calendar;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Set;

public class WorkingCalendar {

    public static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(3, 20),
            MonthDay.of(4, 9),
            MonthDay.of(5, 1),
            MonthDay.of(7, 25),
            MonthDay.of(8, 13),
            MonthDay.of(10, 15),
            MonthDay.of(12, 17));

    private static final int MAX_DAYS_SCANNED = 3660;

    private final Set<LocalDate> movableHolidays;

    public WorkingCalendar(Collection<LocalDate> movableHolidays) {
        this.movableHolidays = Set.copyOf(movableHolidays);
    }

    public boolean isWorkingDay(LocalDate day) {
        DayOfWeek dayOfWeek = day.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY
                && dayOfWeek != DayOfWeek.SUNDAY
                && !FIXED_HOLIDAYS.contains(MonthDay.from(day))
                && !movableHolidays.contains(day);
    }

    public Instant addWorkingTime(Instant start, Duration amount) {
        Instant cursor = start;
        Duration remaining = amount;
        for (int scanned = 0; scanned < MAX_DAYS_SCANNED; scanned++) {
            LocalDate day = cursor.atZone(TUNIS).toLocalDate();
            Instant endOfDay = day.plusDays(1).atStartOfDay(TUNIS).toInstant();
            if (isWorkingDay(day)) {
                Duration available = Duration.between(cursor, endOfDay);
                if (remaining.compareTo(available) <= 0) {
                    return cursor.plus(remaining);
                }
                remaining = remaining.minus(available);
            }
            cursor = endOfDay;
        }
        throw new IllegalStateException("No working days found within ten years of " + start);
    }

    public Duration workingTimeBetween(Instant from, Instant to) {
        Duration total = Duration.ZERO;
        Instant cursor = from;
        while (cursor.isBefore(to)) {
            LocalDate day = cursor.atZone(TUNIS).toLocalDate();
            Instant endOfDay = day.plusDays(1).atStartOfDay(TUNIS).toInstant();
            Instant segmentEnd = endOfDay.isBefore(to) ? endOfDay : to;
            if (isWorkingDay(day)) {
                total = total.plus(Duration.between(cursor, segmentEnd));
            }
            cursor = segmentEnd;
        }
        return total;
    }
}