package org.dentalmanagementsystem.Controller;


import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.AdminRepository;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping("/dashboard")
    public String showPatientDashboard() {
        return "patient/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(Model model) {
        // 1. Fetch all admins from the database
        List<Admin> admins = adminRepository.findAll();
        // 2. Add the list to the Model so Thymeleaf can read it
        model.addAttribute("admins", admins);
        model.addAttribute("activePage", "dashboard");

        return "admin/admin-dashboard";
    }

    @GetMapping("/admin/manage-admins")
    public String showAdminManagementPage(Model model) {
        // Fetch all admins to display in the table
        List<Admin> admins = adminRepository.findAll();
        model.addAttribute("admins", admins);
        model.addAttribute("activePage", "manage-admins");

        // Fetch the currently logged-in admin's email and add it to the model
        String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        model.addAttribute("currentUserEmail", currentUserEmail);

        return "admin/admin-management";
    }

    @GetMapping("/admin/profile")
    public String showAdminProfile(Model model) {
        // Get the currently logged-in user's email from Spring Security
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

        // Fetch the admin object from the database
        Admin admin = adminRepository.findByEmail(email);

        // Add the object to the model so Thymeleaf can read it
        model.addAttribute("admin", admin);
        model.addAttribute("activePage", "profile");

        return "admin/admin-profile";
    }

    @GetMapping("/admin/patients-management")
    public String showPatientManagementPage(Model model) {
        List<Patient> patients = patientRepository.findAll();
        model.addAttribute("patients", patients);

        model.addAttribute("activePage", "patients");

        String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        model.addAttribute("currentUserEmail", currentUserEmail);

        return "admin/patient-management";
    }
}