package org.dentalmanagementsystem.Controller;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Entity.MedicalRecord;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.AppointmentRepository;
import org.dentalmanagementsystem.Repository.DentistRepository;
import org.dentalmanagementsystem.Repository.MedicalRecordRepository;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.dentalmanagementsystem.Service.MedicalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dentist")
@RequiredArgsConstructor
public class DentistMedicalRecordController {

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordService medicalRecordService;
    private final DentistRepository dentistRepository;
    private final PatientRepository patientRepository;

    // ==========================================
    // UI PAGES
    // ==========================================

    // 1. View list of all patients treated by this dentist
    @GetMapping("/my-patients")
    public String viewMyPatients(Principal principal, Model model) {
        // Fetch logged-in dentist
        Dentist dentist = dentistRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Dentist not found"));

        // Fetch distinct patients who have booked this dentist
        List<Patient> patients = appointmentRepository.findDistinctPatientsByDentistId(dentist.getId());
        model.addAttribute("patients", patients);

        return "dentist/my-patients"; // Returns the My Patients UI page
    }

    // 2. View specific patient's comprehensive medical records
    @GetMapping("/patients/{patientId}/records")
    public String viewPatientRecords(@PathVariable Long patientId, Principal principal, Model model) {
        Dentist dentist = dentistRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Dentist not found"));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Fetch all appointments they had with THIS dentist (to link new records to them)
        List<Appointment> appointments = appointmentRepository.findByPatientIdAndDentistIdOrderByAppointmentDateDesc(patientId, dentist.getId());

        // Fetch all medical records created by THIS dentist for THIS patient
        List<MedicalRecord> records = medicalRecordRepository.findByPatientIdAndDentistIdOrderByCreatedAtDesc(patientId, dentist.getId());

        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointments);
        model.addAttribute("records", records);

        return "dentist/patient-records"; // Returns the Patient Records UI page
    }


    // ==========================================
    // REST API ENDPOINTS
    // ==========================================

    // API: Create Record
    @PostMapping("/api/records")
    @ResponseBody
    public ResponseEntity<?> addMedicalRecord(
            @RequestParam Long appointmentId,
            @RequestParam String recordType,
            @RequestParam String notes,
            @RequestParam(required = false) MultipartFile file) {
        try {
            medicalRecordService.createRecord(appointmentId, recordType, notes, file);
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add record: " + e.getMessage());
        }
    }

    // API: Update Record Notes
    @PutMapping("/api/records/{id}")
    @ResponseBody
    public ResponseEntity<?> updateRecord(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            medicalRecordService.updateRecordNotes(id, request.get("notes"));
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update record: " + e.getMessage());
        }
    }

    // API: Delete Record
    @DeleteMapping("/api/records/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteRecord(@PathVariable Long id) {
        try {
            medicalRecordService.deleteRecord(id);
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete record: " + e.getMessage());
        }
    }
}