package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Used by the generation algorithm to filter out taken slots
    List<Appointment> findByDentistAndAppointmentDateAndStatusNot(Dentist dentist, LocalDate date, String status);

    // Used by the controller to prevent double-booking at the exact same millisecond
    boolean existsByDentistAndAppointmentDateAndStartTimeAndStatusNot(Dentist dentist, LocalDate date, LocalTime startTime, String status);

    // Used by the patient dashboard to display their history
    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);

    List<Appointment> findByDentistIdAndAppointmentDateOrderByStartTimeAsc(Long dentistId, LocalDate appointmentDate);

    Optional<Appointment> findFirstByPatientIdAndStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(Long patientId, String status, LocalDate currentDate);
}