package org.dentalmanagementsystem.Controller;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.AdminRepository;
import org.dentalmanagementsystem.Service.AdminService;
import org.dentalmanagementsystem.Service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final AdminRepository adminRepository;

    // Triggered by submitNewAdmin() in JS
    @PostMapping("/create")
    public ResponseEntity<?> createAdmin(@RequestBody Admin admin) {
        try {
            adminService.createAdmin(admin);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Admin created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create admin");
        }
    }

    // Triggered when opening the Edit Modal to populate the fields
    @GetMapping("/{id}")
    public ResponseEntity<?> getAdmin(@PathVariable Long id) {
        try {
            Admin admin = adminService.getAdminById(id);
            return ResponseEntity.ok(admin);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Triggered when submitting the Edit Modal
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        try {
            adminService.updateAdmin(id, admin);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Admin updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update admin");
        }
    }

    // Triggered by the Block/Restore buttons in the table
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            boolean status = request.get("status");
            adminService.toggleAdminStatus(id, status);
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));

            // ADD THIS SPECIFIC CATCH BLOCK:
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update status");
        }
    }

    // Triggered when an admin submits the profile update form
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Admin admin) {
        try {
            // Get the email of whoever is currently logged in via Spring Security
            String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

            adminService.updateMyProfile(currentUserEmail, admin);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Profile updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update profile");
        }
    }

    @PostMapping("/add-patient")
    public ResponseEntity<?> addPatientByAdmin(@RequestBody Patient patient) {

        // 1. Give them a secure random dummy password (they can never guess this)
        patient.setPassword(java.util.UUID.randomUUID().toString());

        // 2. Mark them as NOT verified
        patient.setVerified(false);

        authService.registerNewPatient(patient);
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    // 2. UPDATE PASSWORD
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> request) {
        try {
            String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            Admin existingAdmin = adminRepository.findByEmail(currentEmail);

            if (existingAdmin == null) {
                return ResponseEntity.badRequest().body("User not found.");
            }

            // Update only the password
            existingAdmin.setPassword(request.get("password"));
            adminRepository.save(existingAdmin);

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update password.");
        }
    }

    // 3. DEACTIVATE ACCOUNT
    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivateAccount() {
        try {
            String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            Admin existingAdmin = adminRepository.findByEmail(currentEmail);

            if (existingAdmin != null) {
                // Security Block: Prevent the primary Super Admin from deactivating themselves
                if (existingAdmin.getId() == 1L) {
                    return ResponseEntity.badRequest().body("The primary Super Admin account cannot be deactivated.");
                }

                existingAdmin.setActive(false);
                adminRepository.save(existingAdmin);
                return ResponseEntity.ok(Map.of("status", "SUCCESS"));
            }
            return ResponseEntity.badRequest().body("User not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}