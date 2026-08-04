package org.dentalmanagementsystem.Config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final PatientRepository patientRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract email and name from the social provider's payload
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email != null) {
            Patient existingPatient = patientRepository.findByEmail(email);

            if (existingPatient == null) {
                // SCENARIO 1: Brand new user registering via Social Login
                Patient newPatient = new Patient();
                newPatient.setEmail(email);
                newPatient.setFullName(name);

                // Generate a random placeholder password since they authenticate via OAuth2
                newPatient.setPassword(UUID.randomUUID().toString());

                // IMPORTANT: Since Google verified their email, mark them as verified!
                newPatient.setVerified(true);

                patientRepository.save(newPatient);
                System.out.println("Registered new patient via Social Login: " + email);

            } else if (!existingPatient.isVerified()) {
                // SCENARIO 2: Admin created this user, but they used Google for their first login instead of OTP
                // Since Google authenticated them, we can safely mark their account as verified
                existingPatient.setVerified(true);

                patientRepository.save(existingPatient);
                System.out.println("Verified existing admin-created patient via Social Login: " + email);
            }
            // If existingPatient != null AND isVerified() == true, we do nothing and just let them log in.
        }

        // Redirect to the dashboard after successful login or registration
        response.sendRedirect("/dashboard");
    }
}