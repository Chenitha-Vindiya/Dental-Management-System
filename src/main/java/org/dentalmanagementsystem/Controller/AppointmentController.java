package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.dentalmanagementsystem.Service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;

    public AppointmentController(AppointmentService appointmentService, PatientRepository patientRepository) {
        this.appointmentService = appointmentService;
        this.patientRepository = patientRepository;
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody Appointment request, Principal principal) {
        try {
            if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

            Patient patient = patientRepository.findByEmail(principal.getName());
            if (patient == null) return ResponseEntity.status(401).body("Patient not found");

            // Link the appointment to the logged-in patient
            request.setPatient(patient);

            appointmentService.bookAppointment(request);

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error booking appointment: " + e.getMessage());
        }
    }
}