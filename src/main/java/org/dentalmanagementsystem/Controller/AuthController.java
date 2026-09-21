package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Repository.AdminRepository;
import org.dentalmanagementsystem.Repository.DentistRepository;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.dentalmanagementsystem.Service.AuthService;
import org.dentalmanagementsystem.Entity.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private DentistRepository dentistRepository;

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // 1. IS IT AN ADMIN?
        if (email.toLowerCase().endsWith("@admin.com")) {
            Admin admin = adminRepository.findByEmail(email);
            if (admin != null) {
                // ADD THIS BLOCK: Block Deactivated Admins
                if (!admin.getActive()) {
                    return ResponseEntity.ok(Map.of("status", "DEACTIVATED_ADMIN"));
                }
                // Admin exists and is active, ask for password
                return ResponseEntity.ok(Map.of("status", "EXISTS_ADMIN"));
            } else {
                // Admins cannot register themselves! Block it.
                return ResponseEntity.ok(Map.of("status", "UNAUTHORIZED_ADMIN"));
            }
        }

        // NEW BLOCK: Check for Dentist
        if (email.toLowerCase().endsWith("@dentist.com")) {
            Dentist dentist = dentistRepository.findByEmail(email);
            if (dentist != null) {
                if (!dentist.isActive()) {
                    return ResponseEntity.ok(Map.of("status", "DEACTIVATED_DENTIST"));
                }
                return ResponseEntity.ok(Map.of("status", "EXISTS_DENTIST"));
            } else {
                return ResponseEntity.ok(Map.of("status", "UNAUTHORIZED_DENTIST"));
            }
        }

        // 2. IF NOT ADMIN, IT MUST BE A PATIENT
        Patient patient = patientRepository.findByEmail(email);
        if (patient != null) {
//            if (!patient.isActive()) {
//                return ResponseEntity.ok(Map.of("status", "DEACTIVATED_PATIENT"));
//            }
            // NEW: Check the boolean flag instead of the password
            if (patient.isVerified()) {
                return ResponseEntity.ok(Map.of("status", "EXISTS")); // Normal login
            } else {
                // Admin created them, but they haven't verified and set their real password yet
                return ResponseEntity.ok(Map.of("status", "NEEDS_PASSWORD"));
            }
        }
        return ResponseEntity.ok(Map.of("status", "NEW_USER"));
    }

    //For saving ONLY the password after OTP
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody Map<String, String> request) {
        Patient patient = patientRepository.findByEmail(request.get("email"));
        if (patient != null) {
            patient.setPassword(request.get("password"));

            // IMPORTANT: Mark them as verified now!
            patient.setVerified(true);

            patientRepository.save(patient);
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        }
        return ResponseEntity.badRequest().body("User not found");
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        try {
            // This calls your beautifully formatted email method!
            authService.generateAndSendOtp(email);

            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "ERROR", "message", "Failed to send OTP: " + e.getMessage()));
        }
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
        // Since public users verify OTP BEFORE reaching this step, they are verified immediately
        patient.setVerified(true);
        authService.registerNewPatient(patient);
        return ResponseEntity.ok(Map.of("status", "REGISTERED"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        // 1. ADMIN LOGIN LOGIC
        if (email.toLowerCase().endsWith("@admin.com")) {
            Admin admin = adminRepository.findByEmail(email);
            if (admin != null && admin.getPassword().equals(password)) {

                if (!admin.getActive()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account has been deactivated. Please contact a Super Admin.");
                }
                // --- CREATE THE SPRING SECURITY SESSION ---
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                HttpSession session = request.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                // ------------------------------------------

                // Tell frontend to redirect to admin dashboard
                return ResponseEntity.ok(Map.of("redirect", "/admin/dashboard"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin credentials");
        }

        // NEW BLOCK: DENTIST LOGIN LOGIC
        if (email.toLowerCase().endsWith("@dentist.com")) {
            Dentist dentist = dentistRepository.findByEmail(email);
            if (dentist != null && dentist.getPassword().equals(password)) {

                if (!dentist.isActive()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account is deactivated.");
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_DENTIST")));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                HttpSession session = request.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                
                if (dentist.getConsultationFee() == null || dentist.getConsultationFee() <= 0) {
                    dentist.setConsultationFee(2500.00);
                }

                return ResponseEntity.ok(Map.of("redirect", "/dentist/dashboard"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid dentist credentials");
        }

        // 2. PATIENT LOGIN LOGIC
        Patient patient = patientRepository.findByEmail(email);
        if (patient != null && patient.getPassword().equals(password)) {

            // --- CREATE THE SPRING SECURITY SESSION ---
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            // ------------------------------------------

            // Tell frontend to redirect to patient dashboard
            return ResponseEntity.ok(Map.of("redirect", "/dashboard"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid patient credentials");
    }
}