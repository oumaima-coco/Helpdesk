package io.github.oumaimacoco.helpdesk.calendar;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    List<PublicHoliday> findAllByOrderByHolidayDateAsc();

    boolean existsByHolidayDate(LocalDate holidayDate);
}