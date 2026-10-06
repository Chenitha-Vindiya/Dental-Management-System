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
import org.springframework.beans.factory.annotation.Autowired;
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
public class MedicalRecordController {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;
    @Autowired
    private MedicalRecordService medicalRecordService;
    @Autowired
    private DentistRepository dentistRepository;
    @Autowired
    private PatientRepository patientRepository;

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

    @PutMapping("/api/records/{id}/with-file")
    @ResponseBody
    public ResponseEntity<?> updateRecordWithFile(
            @PathVariable Long id,
            @RequestParam("notes") String notes,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            medicalRecordService.updateRecordWithFile(id, notes, file);
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update record: " + e.getMessage());
        }
    }
}