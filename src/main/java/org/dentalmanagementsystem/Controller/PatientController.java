package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/patient/")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @PutMapping("profile/info")
    public ResponseEntity<?> updatePersonalInfo(@RequestBody Map<String, String> requestData, Principal principal) {
        try {
            if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

            Patient patient = patientService.getPatientByEmail(principal.getName());
            if (patient == null) return ResponseEntity.status(401).body("Patient not found");

            patientService.updatePatientInfo(patient, requestData.get("fullName"), requestData.get("phoneNumber"));

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update profile.");
        }
    }

    @PutMapping("profile/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> requestData, Principal principal) {
        try {
            if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

            Patient patient = patientService.getPatientByEmail(principal.getName());
            if (patient == null) return ResponseEntity.status(401).body("Patient not found");

            patientService.updatePassword(patient, requestData.get("password"));

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update password.");
        }
    }

    @PostMapping("profile/deactivate")
    public ResponseEntity<?> deactivateAccount(Principal principal) {
        try {
            if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

            Patient patient = patientService.getPatientByEmail(principal.getName());
            if (patient == null) return ResponseEntity.status(401).body("Patient not found");

            patientService.deactivateAccount(patient);

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to deactivate account.");
        }
    }
}