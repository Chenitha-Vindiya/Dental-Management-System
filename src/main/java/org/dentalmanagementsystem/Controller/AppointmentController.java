package org.dentalmanagementsystem.Controller;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.*;
import org.dentalmanagementsystem.Repository.*;
import org.dentalmanagementsystem.Service.AppointmentService;
import org.dentalmanagementsystem.Service.DentistScheduleService; // Import service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DentistRepository dentistRepository;

    @Autowired
    private DentistScheduleService dentistScheduleService;

    @Autowired
    private PaymentRecordRepository paymentRepository;

    @GetMapping("/available")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam Long dentistId,
            @RequestParam String date) {

        LocalDate requestedDate = LocalDate.parse(date);
        Dentist dentist = dentistRepository.findById(dentistId).orElseThrow();

        // FIXED: Use getEffectiveScheduleForDate to check for temporary date-specific overrides first,
        // falling back to the weekly schedule only if no temporary override exists for this exact date.
        DentistSchedule schedule = dentistScheduleService.getEffectiveScheduleForDate(dentist.getId(), requestedDate);

        List<Appointment> bookedAppointments = appointmentRepository.findByDentistAndAppointmentDateAndStatusNot(dentist, requestedDate, "CANCELLED");

        List<LocalTime> availableSlots = appointmentService.generateAvailableSlots(schedule, requestedDate, bookedAppointments);

        return ResponseEntity.ok(availableSlots);
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody Map<String, String> request, Principal principal) {
        try {
            Patient patient = patientRepository.findByEmail(principal.getName());

            Long dentistId = Long.parseLong(request.get("dentistId"));
            LocalDate appointmentDate = LocalDate.parse(request.get("appointmentDate"));
            LocalTime startTime = LocalTime.parse(request.get("startTime"));
            String serviceType = request.get("serviceType");
            String patientNotes = request.get("patientNotes");
            String paymentMethod = request.get("paymentMethod");

            Dentist dentist = dentistRepository.findById(dentistId)
                    .orElseThrow(() -> new RuntimeException("Dentist not found."));

            // Additional Safety Check: Ensure the requested slot falls within the effective schedule for that date
            DentistSchedule effectiveSchedule = dentistScheduleService.getEffectiveScheduleForDate(dentist.getId(), appointmentDate);
            if (!effectiveSchedule.isWorkingDay() || startTime.isBefore(effectiveSchedule.getStartTime()) || startTime.isAfter(effectiveSchedule.getEndTime())) {
                return ResponseEntity.badRequest().body("The dentist is not available on this date or time.");
            }

            // Concurrency Guard
            boolean conflict = appointmentRepository.existsByDentistAndAppointmentDateAndStartTimeAndStatusNot(
                    dentist, appointmentDate, startTime, "CANCELLED"
            );

            if (conflict) {
                return ResponseEntity.badRequest().body("This time slot has just been booked by another patient.");
            }

            // Save Appointment
            Appointment apt = new Appointment();
            apt.setPatient(patient);
            apt.setDentist(dentist);
            apt.setAppointmentDate(appointmentDate);
            apt.setStartTime(startTime);
            apt.setEndTime(startTime.plusMinutes(50));
            apt.setServiceType(serviceType);
            apt.setPatientNotes(patientNotes);
            apt.setStatus("SCHEDULED");
            appointmentRepository.save(apt);

            // Save Payment Record
            PaymentRecord payment = new PaymentRecord();
            payment.setAppointment(apt);
            payment.setAmount(dentist.getConsultationFee());
            payment.setMethod(paymentMethod);

            if ("ONLINE".equals(paymentMethod)) {
                payment.setStatus("PENDING_ONLINE");
            } else if ("BANK_TRANSFER".equals(paymentMethod)) {
                payment.setStatus("PENDING_TRANSFER");
            } else {
                payment.setStatus("PENDING_VISIT");
            }

            paymentRepository.save(payment);
            appointmentService.sendAppointmentConfirmation(
                    patient.getEmail(),
                    patient.getFullName(),
                    dentist.getFullName(),
                    apt.getAppointmentDate(),
                    apt.getStartTime()
            );

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process booking: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeAppointment(@PathVariable Long id) {
        Appointment apt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        LocalDateTime appointmentEndDateTime = LocalDateTime.of(apt.getAppointmentDate(), apt.getEndTime());

        if (LocalDateTime.now().isBefore(appointmentEndDateTime)) {
            return ResponseEntity.badRequest().body("Cannot complete an appointment before its end time has passed.");
        }

        apt.setStatus("COMPLETED");
        appointmentRepository.save(apt);

        return ResponseEntity.ok().body("Appointment completed successfully.");
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleAppointment(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Appointment apt = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            LocalDate newDate = LocalDate.parse(request.get("appointmentDate"));
            LocalTime newStartTime = LocalTime.parse(request.get("startTime"));

            // Check availability conflict
            boolean conflict = appointmentRepository.existsByDentistAndAppointmentDateAndStartTimeAndStatusNot(
                    apt.getDentist(), newDate, newStartTime, "CANCELLED"
            );

            if (conflict) {
                return ResponseEntity.badRequest().body("This time slot is already booked.");
            }

            apt.setAppointmentDate(newDate);
            apt.setStartTime(newStartTime);
            apt.setEndTime(newStartTime.plusMinutes(50));
            apt.setStatus("RESCHEDULED");
            appointmentRepository.save(apt);

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to reschedule: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {
        try {
            Appointment apt = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            apt.setStatus("CANCELLED");
            appointmentRepository.save(apt);

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to cancel appointment: " + e.getMessage());
        }
    }
}