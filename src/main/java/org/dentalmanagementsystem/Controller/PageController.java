package org.dentalmanagementsystem.Controller;


import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.AdminRepository;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PatientRepository patientRepository;

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
        model.addAttribute("nextAppointment", null); // Passing null for testing the empty state

        return "patient/dashboard";
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
}