package org.dentalmanagementsystem.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Service.DentistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/dentist")
@RequiredArgsConstructor
public class DentistController {

    @Autowired
    private DentistService dentistService;

    @PutMapping("/profile/info")
    public ResponseEntity<?> updateProfileInfo(@RequestBody Map<String, String> request, Principal principal) {
        try {
            // Get the securely authenticated user's email
            String email = principal.getName();

            // Extract the new data from the JSON payload
            String fullName = request.get("fullName");
            String phoneNumber = request.get("phoneNumber");

            // Validate the required fields
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Full Name cannot be empty.");
            }

            // Update database via service
            dentistService.updateProfileInfo(email, fullName, phoneNumber);

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update profile: " + e.getMessage());
        }
    }

    @PutMapping("/profile/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> request, Principal principal) {
        try {
            // Get the securely authenticated user's email
            String email = principal.getName();

            // Extract the new password from the JSON payload
            String newPassword = request.get("password");

            // Validate the password
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password cannot be empty.");
            }

            // Update database via service
            dentistService.updatePassword(email, newPassword);

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update password: " + e.getMessage());
        }
    }

    @PostMapping("/profile/deactivate")
    public ResponseEntity<?> deactivateAccount(Principal principal, HttpServletRequest request) {
        try {
            String email = principal.getName();

            // Deactivate in database
            dentistService.deactivateAccount(email);

            // Destroy the current Spring Security session
            request.getSession().invalidate();

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to deactivate account: " + e.getMessage());
        }
    }

}