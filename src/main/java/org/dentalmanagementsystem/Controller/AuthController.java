package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Service.AuthService;
import org.dentalmanagementsystem.Entity.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (authService.checkEmailExists(email)) {
            return ResponseEntity.ok(Map.of("status", "EXISTS"));
        } else {
            authService.generateAndSendOtp(email);
            return ResponseEntity.ok(Map.of("status", "NEW_USER"));
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
        authService.registerNewPatient(patient);
        return ResponseEntity.ok(Map.of("status", "REGISTERED"));
    }
}