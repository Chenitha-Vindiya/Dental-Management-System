package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Service.DentistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dentists")
public class AdminDentistController {

    private final DentistService dentistService;

    public AdminDentistController(DentistService dentistService) {
        this.dentistService = dentistService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createDentist(@RequestBody Dentist dentist) {
        try {
            dentistService.createDentist(dentist);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Dentist added successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add dentist.");
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            dentistService.toggleDentistStatus(id, request.get("status"));
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (IllegalStateException e) {
            // Catch the specific exception thrown by the service when appointments exist
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update status.");
        }
    }
}