package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Repository.AdminRepository;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.dentalmanagementsystem.Service.AuthService;
import org.dentalmanagementsystem.Entity.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PatientRepository patientRepository;
    private final AdminRepository adminRepository;

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // 1. IS IT AN ADMIN?
        if (email.toLowerCase().endsWith("@admin.com")) {
            Admin admin = adminRepository.findByEmail(email);
            if (admin != null) {
                // Admin exists, ask for password
                return ResponseEntity.ok(Map.of("status", "EXISTS_ADMIN"));
            } else {
                // Admins cannot register themselves! Block it.
                return ResponseEntity.ok(Map.of("status", "UNAUTHORIZED_ADMIN"));
            }
        }

        // 2. IF NOT ADMIN, IT MUST BE A PATIENT
        Patient patient = patientRepository.findByEmail(email);
        if (patient != null) {
            if (patient.getPassword() != null) {
                return ResponseEntity.ok(Map.of("status", "EXISTS")); // Has password
            } else {
                return ResponseEntity.ok(Map.of("status", "NEW_USER")); // Social login only, or incomplete
            }
        }

        // Brand new patient
        return ResponseEntity.ok(Map.of("status", "NEW_USER"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        boolean isValid = authService.verifyOtp(request.get("email"), request.get("otp"));
        if (isValid) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        }
        return ResponseEntity.badRequest().body(Map.of("status", "INVALID_OTP"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Patient patient) {
        authService.registerNewPatient(patient);
        return ResponseEntity.ok(Map.of("status", "REGISTERED"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        // 1. ADMIN LOGIN LOGIC
        if (email.toLowerCase().endsWith("@admin.com")) {
            Admin admin = adminRepository.findByEmail(email);
            if (admin != null && admin.getPassword().equals(password)) {
                // Tell frontend to redirect to admin dashboard
                return ResponseEntity.ok(Map.of("redirect", "/admin/dashboard"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin credentials");
        }

        // 2. PATIENT LOGIN LOGIC
        Patient patient = patientRepository.findByEmail(email);
        if (patient != null && patient.getPassword().equals(password)) {
            // Tell frontend to redirect to patient dashboard
            return ResponseEntity.ok(Map.of("redirect", "/dashboard"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid patient credentials");
    }
}