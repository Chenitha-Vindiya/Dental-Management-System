package org.dentalmanagementsystem.Controller;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

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
}