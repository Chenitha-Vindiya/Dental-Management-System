package org.dentalmanagementsystem.Controller;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.*;
import org.dentalmanagementsystem.Repository.*;
import org.dentalmanagementsystem.Service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
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
    private DentistScheduleRepository dentistScheduleRepository;

    @Autowired
    private PaymentRecordRepository paymentRepository;

    @GetMapping("/available")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam Long dentistId,
            @RequestParam String date) {

        LocalDate requestedDate = LocalDate.parse(date);
        Dentist dentist = dentistRepository.findById(dentistId).orElseThrow();

        DentistSchedule schedule = dentistScheduleRepository.findByDentistAndDayOfWeek(dentist, requestedDate.getDayOfWeek());
        List<Appointment> bookedAppointments = appointmentRepository.findByDentistAndAppointmentDateAndStatusNot(dentist, requestedDate, "CANCELLED");

        List<LocalTime> availableSlots = appointmentService.generateAvailableSlots(schedule, requestedDate, bookedAppointments);

        return ResponseEntity.ok(availableSlots);
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody Map<String, String> request, Principal principal) {
        try {
            Patient patient = patientRepository.findByEmail(principal.getName());

            // 1. Parse the strings from the JSON Map
            Long dentistId = Long.parseLong(request.get("dentistId"));
            LocalDate appointmentDate = LocalDate.parse(request.get("appointmentDate"));
            LocalTime startTime = LocalTime.parse(request.get("startTime"));
            String serviceType = request.get("serviceType");
            String patientNotes = request.get("patientNotes");
            String paymentMethod = request.get("paymentMethod");

            Dentist dentist = dentistRepository.findById(dentistId)
                    .orElseThrow(() -> new RuntimeException("Dentist not found."));

            // 2. Concurrency Guard
            boolean conflict = appointmentRepository.existsByDentistAndAppointmentDateAndStartTimeAndStatusNot(
                    dentist, appointmentDate, startTime, "CANCELLED"
            );

            if (conflict) {
                return ResponseEntity.badRequest().body("This time slot has just been booked by another patient.");
            }

            // 3. Save Appointment
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

            // 4. Save Payment Record
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
}