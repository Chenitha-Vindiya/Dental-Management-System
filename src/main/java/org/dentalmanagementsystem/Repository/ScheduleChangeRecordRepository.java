package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.ScheduleChangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleChangeRecordRepository extends JpaRepository<ScheduleChangeRecord, Long> {

    // Checks if a pending request already contains this target date for this dentist
    @Query("SELECT COUNT(r) > 0 FROM ScheduleChangeRecord r " +
            "JOIN r.scheduleChangeRequest req " +
            "WHERE req.dentist.id = :dentistId " +
            "AND req.status = 'PENDING' " +
            "AND r.targetDate = :targetDate")
    boolean existsPendingByDentistAndDate(@Param("dentistId") Long dentistId, @Param("targetDate") LocalDate targetDate);

    // Fetch an approved temporary override for a specific date
    @Query("SELECT r FROM ScheduleChangeRecord r WHERE r.scheduleChangeRequest.dentist.id = :dentistId " +
            "AND r.targetDate = :targetDate " +
            "AND r.scheduleChangeRequest.status = 'APPROVED' " +
            "AND r.changeType = 'TEMPORARY'")
    Optional<ScheduleChangeRecord> findApprovedTemporaryOverride(@Param("dentistId") Long dentistId, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT r FROM ScheduleChangeRecord r JOIN r.scheduleChangeRequest req WHERE req.dentist.id = :dentistId AND r.targetDate BETWEEN :startDate AND :endDate")
    List<ScheduleChangeRecord> findByDentistIdAndTargetDateBetween(
            @Param("dentistId") Long dentistId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}