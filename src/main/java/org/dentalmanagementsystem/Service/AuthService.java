package org.dentalmanagementsystem.Service;

import org.dentalmanagementsystem.Entity.*;
import org.dentalmanagementsystem.Repository.*;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PatientRepository patientRepository;
    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender; // Injected Mail Sender

    public boolean checkEmailExists(String email) {
        return patientRepository.existsByEmail(email);
    }

    public void generateAndSendOtp(String email) {
        // Generate 6 digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        OtpRecord record = new OtpRecord();
        record.setEmail(email);
        record.setOtpCode(otp);
        record.setExpirationTime(LocalDateTime.now().plusMinutes(5)); // Valid for 5 mins
        otpRepository.save(record);

        // Send the actual email
        CompletableFuture.runAsync(() -> sendEmail(email, otp));
    }

    private void sendEmail(String toEmail, String otp) {
        try {
            // Create a MimeMessage instead of a SimpleMailMessage
            MimeMessage message = mailSender.createMimeMessage();

            // Use MimeMessageHelper to easily set the email details and enable HTML
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("AuraDent - Your Secure Verification Code");

            // Professional HTML Template matching the AuraDent brand
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f9f9ff; color: #111c2d;">
                    
                    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f9f9ff; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <!-- Main Card -->
                                <table width="100%" max-width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 16px; border: 1px solid #e7eeff; box-shadow: 0 10px 25px rgba(0, 90, 183, 0.05); overflow: hidden; max-width: 550px; margin: 0 auto;">
                                    
                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="padding: 32px 24px; border-bottom: 1px solid #f0f3ff;">
                                            <h2 style="color: #005ab7; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">AuraDent</h2>
                                        </td>
                                    </tr>
                                    
                                    <!-- Body Content -->
                                    <tr>
                                        <td style="padding: 40px 32px;">
                                            <p style="margin: 0 0 16px 0; font-size: 16px; color: #414754; line-height: 1.5;">
                                                Hello,
                                            </p>
                                            <p style="margin: 0 0 24px 0; font-size: 16px; color: #414754; line-height: 1.5;">
                                                Please use the verification code below to securely access your AuraDent account. This code is valid for the next <strong>5 minutes</strong>.
                                            </p>
                                            
                                            <!-- OTP Box -->
                                            <div style="text-align: center; margin: 32px 0;">
                                                <span style="display: inline-block; font-size: 32px; font-weight: 700; color: #005ab7; letter-spacing: 8px; padding: 16px 32px; background-color: #e7eeff; border-radius: 12px; border: 1px solid #d8e3fb;">
                                                    %s
                                                </span>
                                            </div>
                                            
                                            <p style="margin: 0; font-size: 14px; color: #717680; line-height: 1.5;">
                                                If you did not request this code, you can safely ignore this email. Your account remains secure.
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="padding: 24px; background-color: #fcfcfd; border-top: 1px solid #f0f3ff;">
                                            <p style="margin: 0; font-size: 12px; color: #a0a5b1;">
                                                &copy; 2026 AuraDent System. Clinical Transparency in Dental Care.<br>
                                                This is an automated message, please do not reply.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.replace("%s", otp); // This inserts the generated OTP right into the %s placeholder

            // Set the second parameter to 'true' to tell Spring Boot this is HTML, not plain text
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("Professional HTML email successfully sent to: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            // You might want to throw a custom exception here later depending on how you handle errors
        }
    }

    public boolean verifyOtp(String email, String otpCode) {
        OtpRecord record = otpRepository.findTopByEmailOrderByExpirationTimeDesc(email);

        if (record != null &&
                record.getOtpCode().equals(otpCode) &&
                record.getExpirationTime().isAfter(LocalDateTime.now())) {
            return true;
        }
        return false;
    }

    public void registerNewPatient(Patient patient) {
        patientRepository.save(patient);
    }
}