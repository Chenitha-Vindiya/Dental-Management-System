package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.ScheduleChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleChangeRequestRepository extends JpaRepository<ScheduleChangeRequest, Long> {

    // For Admin: View all pending requests
    List<ScheduleChangeRequest> findByStatus(ScheduleChangeRequest.RequestStatus status);

    // CORE LOGIC: Find requests ordered by newest ID
    @Query("SELECT r FROM ScheduleChangeRequest r WHERE r.dentist.id = :dentistId ORDER BY r.id DESC")
    List<ScheduleChangeRequest> findByDentistIdCustomOrder(@Param("dentistId") Long dentistId);
}