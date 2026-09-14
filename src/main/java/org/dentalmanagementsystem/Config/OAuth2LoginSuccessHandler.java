package org.dentalmanagementsystem.Config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final PatientRepository patientRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        // --- NEW: Extract the profile picture URL ---
        String pictureUrl = oAuth2User.getAttribute("picture");

        if (email != null) {
            Patient existingPatient = patientRepository.findByEmail(email);
            boolean needsSaving = false;

            if (existingPatient == null) {
                Patient newPatient = new Patient();
                newPatient.setEmail(email);
                newPatient.setFullName(name);
                newPatient.setPassword(UUID.randomUUID().toString());
                newPatient.setVerified(true);
                newPatient.setProfilePicture(pictureUrl); // Save the picture

                patientRepository.save(newPatient);
                System.out.println("Registered new patient via Social Login: " + email);
            } else {
                if (!existingPatient.isVerified()) {
                    existingPatient.setVerified(true);
                    needsSaving = true;
                    System.out.println("Verified existing admin-created patient via Social Login: " + email);
                }

                // If they exist but don't have a profile picture yet, update it
                if (existingPatient.getProfilePicture() == null && pictureUrl != null) {
                    existingPatient.setProfilePicture(pictureUrl);
                    needsSaving = true;
                }

                if (needsSaving) {
                    patientRepository.save(existingPatient);
                }
            }
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        response.sendRedirect("/dashboard");
    }
}