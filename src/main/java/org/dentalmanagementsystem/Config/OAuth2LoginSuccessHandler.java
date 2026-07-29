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

        // Check if patient exists, if not, register them automatically
        if (email != null && !patientRepository.existsByEmail(email)) {
            Patient newPatient = new Patient();
            newPatient.setEmail(email);
            newPatient.setFullName(name);

            // Generate a random placeholder password since they authenticate via OAuth2
            newPatient.setPassword(UUID.randomUUID().toString());

            patientRepository.save(newPatient);
            System.out.println("Registered new patient via Social Login: " + email);
        }

        // Redirect to the dashboard after successful login or registration
        response.sendRedirect("/dashboard");
    }
}