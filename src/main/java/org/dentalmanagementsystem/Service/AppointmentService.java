package org.dentalmanagementsystem.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.DentistSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class AppointmentService {

    @Autowired
    private JavaMailSender mailSender;

    public List<LocalTime> generateAvailableSlots(DentistSchedule schedule, LocalDate requestedDate, List<Appointment> bookedAppointments) {
        List<LocalTime> availableSlots = new ArrayList<>();

        // If it's a day off, return empty list
        if (!schedule.isWorkingDay() || schedule.getStartTime() == null) {
            return availableSlots;
        }

        LocalTime currentTime = schedule.getStartTime();
        LocalTime endTime = schedule.getEndTime();
        int sessionCount = 0;
        LocalDateTime now = LocalDateTime.now();

        // Loop until the next 50-minute session would exceed the shift's end time
        while (currentTime.plusMinutes(50).isBefore(endTime) || currentTime.plusMinutes(50).equals(endTime)) {
            LocalTime slotStart = currentTime;
            LocalTime slotEnd = currentTime.plusMinutes(50);

            boolean isValidTime = true;

            // The 5-Minute Cutoff Rule
            if (requestedDate.equals(now.toLocalDate())) {
                if (now.toLocalTime().plusMinutes(5).isAfter(slotStart)) {
                    isValidTime = false; // Too close or already passed
                }
            } else if (requestedDate.isBefore(now.toLocalDate())) {
                isValidTime = false; // Past dates
            }

            // Check if already booked in the database
            boolean isBooked = bookedAppointments.stream()
                    .anyMatch(a -> a.getStartTime().equals(slotStart) && !a.getStatus().equals("CANCELLED"));

            if (isValidTime && !isBooked) {
                availableSlots.add(slotStart);
            }

            // Advance time: 50 min session + 10 min gap
            currentTime = slotEnd.plusMinutes(10);
            sessionCount++;

            // 60-Minute break after every 5 sessions
            if (sessionCount % 5 == 0) {
                currentTime = currentTime.plusMinutes(60);
            }
        }
        return availableSlots;
    }

    public void sendAppointmentConfirmation(String toEmail, String patientName, String dentistName, LocalDate date, LocalTime time) {
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(toEmail);
                helper.setSubject("AuraDent - Appointment Confirmation");

                String formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
                String formattedTime = time.format(DateTimeFormatter.ofPattern("hh:mm a"));

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
                                <table width="100%" max-width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 16px; border: 1px solid #e7eeff; box-shadow: 0 10px 25px rgba(0, 90, 183, 0.05); overflow: hidden; max-width: 550px; margin: 0 auto;">
                                    
                                    <tr>
                                        <td align="center" style="padding: 32px 24px; border-bottom: 1px solid #f0f3ff;">
                                            <h2 style="color: #005ab7; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">AuraDent</h2>
                                        </td>
                                    </tr>
                                    
                                    <tr>
                                        <td style="padding: 40px 32px;">
                                            <p style="margin: 0 0 16px 0; font-size: 16px; color: #414754; line-height: 1.5;">
                                                Hello <strong>%s</strong>,
                                            </p>
                                            <p style="margin: 0 0 24px 0; font-size: 16px; color: #414754; line-height: 1.5;">
                                                Your dental appointment has been successfully scheduled. Please review your booking details below:
                                            </p>
                                            
                                            <div style="background-color: #e7eeff; border-radius: 12px; padding: 24px; margin-bottom: 24px; border: 1px solid #d8e3fb;">
                                                <p style="margin: 0 0 12px 0; font-size: 15px; color: #111c2d;">
                                                    <strong>Date:</strong> %s
                                                </p>
                                                <p style="margin: 0 0 12px 0; font-size: 15px; color: #111c2d;">
                                                    <strong>Time:</strong> %s
                                                </p>
                                                <p style="margin: 0; font-size: 15px; color: #111c2d;">
                                                    <strong>Practitioner:</strong> %s
                                                </p>
                                            </div>
                                            
                                            <p style="margin: 0; font-size: 14px; color: #717680; line-height: 1.5;">
                                                If you need to reschedule or cancel, please contact the clinic at least 24 hours in advance.
                                            </p>
                                        </td>
                                    </tr>
                                    
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
                """.formatted(patientName, formattedDate, formattedTime, dentistName);

                helper.setText(htmlContent, true);
                mailSender.send(message);

            } catch (MessagingException e) {
                System.err.println("Failed to send appointment email: " + e.getMessage());
            }
        });
    }
}