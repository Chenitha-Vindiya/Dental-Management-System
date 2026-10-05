package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.ScheduleChangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ScheduleChangeRecordRepository extends JpaRepository<ScheduleChangeRecord, Long> {

    // Checks if a pending request already contains this target date for this dentist
    @Query("SELECT COUNT(r) > 0 FROM ScheduleChangeRecord r " +
            "JOIN r.scheduleChangeRequest req " +
            "WHERE req.dentist.id = :dentistId " +
            "AND req.status = 'PENDING' " +
            "AND r.targetDate = :targetDate")
    boolean existsPendingByDentistAndDate(@Param("dentistId") Long dentistId, @Param("targetDate") LocalDate targetDate);
}