package org.dentalmanagementsystem.Service;

import jakarta.annotation.PostConstruct;
import org.dentalmanagementsystem.Entity.Admin;
import org.dentalmanagementsystem.Entity.AdminRole;
import org.dentalmanagementsystem.Repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    // This annotation makes the method run automatically on application startup
    @PostConstruct
    public void initSuperAdmin() {

        // Check if the admins table is empty
        if (adminRepository.count() == 0) {
            Admin superAdmin = new Admin();
            superAdmin.setEmail("super@admin.com");
            superAdmin.setFullName("AuraDent Admin");

            // NOTE: We will encrypt this with BCrypt later when we set up the admin login
            superAdmin.setPassword("Admin@123");

            superAdmin.setRole(AdminRole.SUPER_ADMIN);

            adminRepository.save(superAdmin);

            System.out.println("========== FIRST TIME SETUP ==========");
            System.out.println("Super Admin automatically created!");
            System.out.println("Email: super@admin.com");
            System.out.println("Role: SUPER_ADMIN");
            System.out.println("======================================");
        }
    }
}