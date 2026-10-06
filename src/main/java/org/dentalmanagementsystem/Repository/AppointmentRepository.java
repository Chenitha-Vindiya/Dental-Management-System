package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Entity.MedicalRecord;
import org.dentalmanagementsystem.Entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
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

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " + "WHERE a.dentist.id = :dentistId " + "AND a.appointmentDate >= :today " + "AND a.status IN ('CONFIRMED', 'SCHEDULED')")
    boolean existsUpcomingAppointmentsForDentist(@Param("dentistId") Long dentistId, @Param("today") LocalDate today);

    List<Appointment> findByDentistIdAndAppointmentDateGreaterThanEqualAndStatusIn(Long dentistId, LocalDate appointmentDate, List<String> statuses);

    List<Appointment> findByDentistIdAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(Long dentistId, LocalDate startDate, LocalDate endDate);

    List<Appointment> findByDentistIdAndAppointmentDateAndStatusIn(Long dentistId, LocalDate appointmentDate, List<String> statuses);

    @Query("SELECT DISTINCT a.patient FROM Appointment a WHERE a.dentist.id = :dentistId")
    List<Patient> findDistinctPatientsByDentistId(@Param("dentistId") Long dentistId);

    List<Appointment> findByPatientIdAndDentistIdOrderByAppointmentDateDesc(Long patientId, Long dentistId);
}