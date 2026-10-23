package io.github.oumaimacoco.helpdesk.calendar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkingCalendarProvider {

    private final PublicHolidayRepository holidays;

    public WorkingCalendarProvider(PublicHolidayRepository holidays) {
        this.holidays = holidays;
    }

    @Transactional(readOnly = true)
    public WorkingCalendar current() {
        return new WorkingCalendar(holidays.findAllByOrderByHolidayDateAsc().stream()
                .map(PublicHoliday::getHolidayDate)
                .toList());
    }
}