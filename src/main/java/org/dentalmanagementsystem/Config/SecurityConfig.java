package org.dentalmanagementsystem.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Publicly accessible paths
                        .requestMatchers("/uploads/**", "/images/**","/", "/auth", "/api/auth/**", "/css/**", "/js/**").permitAll()

                        // SECURE ALL ADMIN PAGES AND APIS
                        .requestMatchers("/admin/**", "/api/admin/**").authenticated()

                        // Any other request must also be authenticated
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth")
                        // Add the custom success handler here
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .csrf(csrf -> csrf.disable()); // Disabled for ease of testing the /api/auth endpoints

        return http.build();
    }
}