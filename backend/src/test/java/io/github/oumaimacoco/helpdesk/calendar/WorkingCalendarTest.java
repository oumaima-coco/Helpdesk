package io.github.oumaimacoco.helpdesk.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkingCalendarTest {

    private static final Duration THREE_DAYS = Duration.ofDays(3);

    private final WorkingCalendar calendar = new WorkingCalendar(List.of());

    @Test
    void threeWorkingDaysFromMondayEndOnThursday() {
        assertThat(calendar.addWorkingTime(at("2026-10-19T10:00"), THREE_DAYS))
                .isEqualTo(at("2026-10-22T10:00"));
    }

    @Test
    void weekendsDoNotCount() {
        assertThat(calendar.addWorkingTime(at("2026-10-22T10:00"), THREE_DAYS))
                .isEqualTo(at("2026-10-27T10:00"));
    }

    @Test
    void fixedHolidaysDoNotCount() {
        assertThat(calendar.isWorkingDay(LocalDate.of(2026, 4, 9))).isFalse();
        assertThat(calendar.addWorkingTime(at("2026-04-08T10:00"), THREE_DAYS))
                .isEqualTo(at("2026-04-14T10:00"));
    }

    @Test
    void revolutionDayIsDecemberSeventeenthNotJanuaryFourteenth() {
        assertThat(calendar.isWorkingDay(LocalDate.of(2026, 12, 17))).isFalse();
        assertThat(calendar.isWorkingDay(LocalDate.of(2027, 1, 14))).isTrue();
    }

    @Test
    void movableHolidaysEnteredByTheAdminDoNotCount() {
        WorkingCalendar withEid = new WorkingCalendar(List.of(
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 5, 28)));

        assertThat(withEid.addWorkingTime(at("2026-05-26T10:00"), THREE_DAYS))
                .isEqualTo(at("2026-06-02T10:00"));
    }

    @Test
    void startingOnAWeekendWaitsForMonday() {
        assertThat(calendar.addWorkingTime(at("2026-10-24T10:00"), Duration.ofDays(1)))
                .isEqualTo(at("2026-10-27T00:00"));
    }

    @Test
    void workingTimeBetweenSkipsWeekendsAndHolidays() {
        assertThat(calendar.workingTimeBetween(at("2026-10-23T12:00"), at("2026-10-26T12:00")))
                .isEqualTo(Duration.ofHours(24));
        assertThat(calendar.workingTimeBetween(at("2026-10-12T00:00"), at("2026-10-19T00:00")))
                .isEqualTo(Duration.ofDays(4));
    }

    private static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(WorkingCalendar.TUNIS).toInstant();
    }
}