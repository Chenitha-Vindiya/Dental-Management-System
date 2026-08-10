package org.dentalmanagementsystem.Service;

import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.AppointmentStatus;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public void bookAppointment(Appointment appointment) {
        // Set defaults for a new booking
        appointment.setStatus(AppointmentStatus.UPCOMING);
        appointment.setDurationMinutes(45);
        appointmentRepository.save(appointment);
    }

    public List<Appointment> getUpcomingAppointments(Patient patient) {
        return appointmentRepository.findByPatientAndStatusOrderByDateTimeAsc(patient, AppointmentStatus.UPCOMING);
    }

    public List<Appointment> getPastAppointments(Patient patient) {
        // Fetch anything that is NOT upcoming (e.g., Completed or Cancelled)
        return appointmentRepository.findByPatientAndStatusNotOrderByDateTimeDesc(patient, AppointmentStatus.UPCOMING);
    }
}