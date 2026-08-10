package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.AppointmentStatus;
import org.dentalmanagementsystem.Entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Finds upcoming appointments sorted by closest date first
    List<Appointment> findByPatientAndStatusOrderByDateTimeAsc(Patient patient, AppointmentStatus status);

    // Finds past (completed/cancelled) appointments sorted by most recent first
    List<Appointment> findByPatientAndStatusNotOrderByDateTimeDesc(Patient patient, AppointmentStatus status);
}