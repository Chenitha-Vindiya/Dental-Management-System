package org.dentalmanagementsystem.Controller;


import org.dentalmanagementsystem.Entity.*;
import org.dentalmanagementsystem.Repository.*;
import org.dentalmanagementsystem.Service.DentistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DentistService dentistService;

    @Autowired
    private DentistRepository dentistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @GetMapping({"/", "/index", "/home"})
    public String showIndexPage() {
        return "index";
    }

    @GetMapping("/auth")
    public String showAuthPage() {
        return "auth";
    }

    // --- PATIENT ROUTES ---

    @GetMapping("/dashboard")
    public String showPatientDashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth";
        }

        String email = principal.getName();
        Patient patient = patientRepository.findByEmail(email);

        // Prevent Thymeleaf crash if session is crossed
        if (patient == null) {
            return "redirect:/logout";
        }

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("patient", patient);

        // Fetch the next upcoming scheduled appointment (from today onwards)
        Appointment nextAppointment = appointmentRepository
                .findFirstByPatientIdAndStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
                        patient.getId(), "SCHEDULED", LocalDate.now()
                ).orElse(null);

        model.addAttribute("nextAppointment", nextAppointment);

        return "patient/dashboard";
    }

    @GetMapping("/appointments")
    public String showPatientAppointments(Model model, Principal principal) {
        // Fetch the logged-in patient
        Patient patient = patientRepository.findByEmail(principal.getName());
        model.addAttribute("patient", patient);
        model.addAttribute("activePage", "appointments");

        // Fetch all active dentists for the booking modal dropdown
        List<Dentist> dentists = dentistRepository.findAll().stream()
                .filter(Dentist::isActive)
                .toList(); // Use .collect(Collectors.toList()) if on older Java versions
        model.addAttribute("dentists", dentists);

        // Fetch the patient's appointment history for the data table
        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patient.getId());
        model.addAttribute("appointments", appointments);

        return "patient/appointments";
    }

    @GetMapping("/payment")
    public String viewPatientBilling(Model model, Principal principal) {
        Patient patient = patientRepository.findByEmail(principal.getName());
        model.addAttribute("patient", patient);
        model.addAttribute("activePage", "payment");

        // Fetch payment records associated with the patient's appointments
        List<PaymentRecord> payments = paymentRecordRepository.findByAppointmentPatientIdOrderByCreatedAtDesc(patient.getId());
        model.addAttribute("payments", payments);

        return "patient/payment";
    }


    @GetMapping("/profile")
    public String showPatientProfile(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth";

        Patient patient = patientRepository.findByEmail(principal.getName());

        // Prevent Thymeleaf crash if session is crossed
        if (patient == null) {
            return "redirect:/logout";
        }

        model.addAttribute("activePage", "profile");
        model.addAttribute("patient", patient);
        return "patient/profile";
    }


    // --- ADMIN ROUTES ---

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth";

        Admin admin = adminRepository.findByEmail(principal.getName());

        // Prevent Thymeleaf crash if a Patient session hits this route
        if (admin == null) {
            return "redirect:/logout";
        }

        List<Admin> admins = adminRepository.findAll();
        long totalPatients = patientRepository.count();

        model.addAttribute("totalPatients", totalPatients);
        model.addAttribute("admins", admins);
        model.addAttribute("activePage", "dashboard");

        return "admin/admin-dashboard";
    }

    @GetMapping("/admin/manage-admins")
    public String showAdminManagementPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth";

        Admin admin = adminRepository.findByEmail(principal.getName());

        // Prevent Thymeleaf crash if a Patient session hits this route
        if (admin == null) {
            return "redirect:/logout";
        }

        List<Admin> admins = adminRepository.findAll();
        model.addAttribute("admins", admins);
        model.addAttribute("activePage", "manage-admins");
        model.addAttribute("currentUserEmail", principal.getName());

        return "admin/admin-management";
    }

    @GetMapping("/admin/profile")
    public String showAdminProfile(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth";

        Admin admin = adminRepository.findByEmail(principal.getName());

        // Prevent Thymeleaf crash if a Patient session hits this route
        if (admin == null) {
            return "redirect:/logout";
        }

        model.addAttribute("admin", admin);
        model.addAttribute("activePage", "profile");

        return "admin/admin-profile";
    }

    @GetMapping("/admin/patients-management")
    public String showPatientManagementPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth";

        Admin admin = adminRepository.findByEmail(principal.getName());

        // Prevent Thymeleaf crash if a Patient session hits this route
        if (admin == null) {
            return "redirect:/logout";
        }

        List<Patient> patients = patientRepository.findAll();
        model.addAttribute("patients", patients);
        model.addAttribute("activePage", "patients");
        model.addAttribute("currentUserEmail", principal.getName());

        return "admin/patient-management";
    }

    // Add this underneath your existing Admin routes in PageController
    @GetMapping("/admin/manage-dentists")
    public String showDentistManagementPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth";

        // Use the extractEmail helper if you applied the Google Login fix earlier
        Admin admin = adminRepository.findByEmail(principal.getName());
        if (admin == null) return "redirect:/logout";

        model.addAttribute("dentists", dentistService.getAllDentists());
        model.addAttribute("activePage", "manage-dentists");
        model.addAttribute("currentUserEmail", admin.getEmail());

        return "admin/dentist-management";
    }

    // --- DENTIST ROUTES ---

    @GetMapping("/dentist/dashboard")
    public String viewDashboard(Model model, Principal principal) {
        Dentist dentist = dentistRepository.findByEmail(principal.getName());
        model.addAttribute("dentist", dentist);
        model.addAttribute("activePage", "dashboard");
        return "dentist/dentist-dashboard";
    }

    @GetMapping("/dentist/appointments")
    public String viewAppointments(Model model, Principal principal) {
        Dentist dentist = dentistRepository.findByEmail(principal.getName());
        model.addAttribute("dentist", dentist);
        model.addAttribute("activePage", "appointments");
        return "dentist/appointments";
    }

    @GetMapping("/dentist/profile")
    public String viewProfile(Model model, Principal principal) {
        Dentist dentist = dentistRepository.findByEmail(principal.getName());
        model.addAttribute("dentist", dentist);
        model.addAttribute("activePage", "profile");
        return "dentist/dentist-profile";
    }
}