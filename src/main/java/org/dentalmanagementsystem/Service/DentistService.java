package org.dentalmanagementsystem.Service;

import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Entity.DentistSchedule;
import org.dentalmanagementsystem.Repository.AppointmentRepository;
import org.dentalmanagementsystem.Repository.DentistRepository;
import org.dentalmanagementsystem.Repository.DentistScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DentistService {

    @Autowired
    private DentistRepository dentistRepository;

    @Autowired
    private DentistScheduleRepository dentistScheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Dentist> getAllDentists() {
        return dentistRepository.findAll();
    }

    public void createDentist(Dentist dentist) {
        // Strict Backend Validation: Enforce Domain
        if (!dentist.getEmail().toLowerCase().endsWith("@dentist.com")) {
            throw new IllegalArgumentException("Dentist email must use the @dentist.com domain.");
        }

        if (dentistRepository.existsByEmail(dentist.getEmail())) {
            throw new IllegalArgumentException("A dentist with this email already exists.");
        }

        dentist.setActive(true);
        // Default specialization if none provided
        if (dentist.getSpecialization() == null || dentist.getSpecialization().trim().isEmpty()) {
            dentist.setSpecialization("General Dentistry");
        }

        dentist.setConsultationFee(2500.00);
        dentistRepository.save(dentist);
        createDefaultSchedule(dentist);
    }

    public void toggleDentistStatus(Long id, boolean status) {
        Dentist dentist = dentistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dentist not found."));

        // If attempting to deactivate the dentist, check for active/confirmed appointments
        if (!status) {
            // You can adjust the statuses based on your exact Enum (e.g., SCHEDULED, CONFIRMED)
            boolean hasConfirmedAppointments = appointmentRepository.existsUpcomingAppointmentsForDentist(
                    id,
                    LocalDate.now()
            );

            if (hasConfirmedAppointments) {
                throw new IllegalStateException("Cannot deactivate dentist. They have confirmed upcoming appointments.");
            }
        }

        dentist.setActive(status);
        dentistRepository.save(dentist);
    }

    public void updateProfileInfo(String email, String fullName, String phoneNumber) {
        Dentist dentist = dentistRepository.findByEmail(email);
        if (dentist != null) {
            dentist.setFullName(fullName);
            dentist.setPhoneNumber(phoneNumber);
            dentistRepository.save(dentist);
        } else {
            throw new RuntimeException("Dentist not found.");
        }
    }

    public void updatePassword(String email, String newPassword) {
        Dentist dentist = dentistRepository.findByEmail(email);
        if (dentist != null) {
            dentist.setPassword(newPassword);
            dentistRepository.save(dentist);
        } else {
            throw new RuntimeException("Dentist not found.");
        }
    }

    public void deactivateAccount(String email) {
        Dentist dentist = dentistRepository.findByEmail(email);
        if (dentist != null) {
            dentist.setActive(false);
            dentistRepository.save(dentist);
        } else {
            throw new RuntimeException("Dentist not found.");
        }
    }

    public void createDefaultSchedule(Dentist dentist) {
        for (DayOfWeek day : DayOfWeek.values()) {
            DentistSchedule schedule = new DentistSchedule();
            schedule.setDentist(dentist);
            schedule.setDayOfWeek(day);

            // DayOfWeek.getValue() returns 1 (Monday) to 7 (Sunday)
            if (day.getValue() >= 1 && day.getValue() <= 5) {
                schedule.setWorkingDay(true);
                schedule.setStartTime(LocalTime.of(8, 0));  // 08:00 AM
                schedule.setEndTime(LocalTime.of(17, 0));   // 05:00 PM
            } else {
                // Saturday and Sunday
                schedule.setWorkingDay(false);
                schedule.setStartTime(null);
                schedule.setEndTime(null);
            }

            dentistScheduleRepository.save(schedule);
        }
    }
}