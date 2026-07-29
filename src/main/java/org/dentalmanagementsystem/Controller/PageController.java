package org.dentalmanagementsystem.Controller;


import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    @Autowired
    private AdminRepository adminRepository;

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

        return "admin/dashboard";
    }
}