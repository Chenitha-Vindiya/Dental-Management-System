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

    // 1. CREATE NEW ADMIN
    public void createAdmin(Admin admin) {
        if (adminRepository.existsByEmail(admin.getEmail())) {
            throw new IllegalArgumentException("An administrator with this email already exists.");
        }

        // Force the active status to true so Jackson's null parsing is ignored
        admin.setActive(true);

        // active is true by default from your Entity, createdAt is handled by Hibernate
        adminRepository.save(admin);
    }

    // 2. GET ADMIN BY ID (For the Edit Modal)
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with ID: " + id));
    }

    // 3. UPDATE EXISTING ADMIN
    public void updateAdmin(Long id, Admin updatedData) {
        Admin existingAdmin = getAdminById(id);

        // Check if they are changing the email to one that already exists
        if (!existingAdmin.getEmail().equals(updatedData.getEmail()) &&
                adminRepository.existsByEmail(updatedData.getEmail())) {
            throw new IllegalArgumentException("Email is already taken by another admin.");
        }

        existingAdmin.setFullName(updatedData.getFullName());
        existingAdmin.setEmail(updatedData.getEmail());
        existingAdmin.setRole(updatedData.getRole());

        // Note: Password update logic has been completely removed to prevent unauthorized changes

        adminRepository.save(existingAdmin);
    }

    // 4. TOGGLE ACTIVE STATUS (With Security Protections)
    public void toggleAdminStatus(Long id, boolean newStatus) {
        Admin admin = getAdminById(id);

        // Only run these checks if we are DEACTIVATING the account (newStatus == false)
        if (!newStatus) {

            // Rule 1: Cannot deactivate the primary Super Admin (Database ID 1)
            if (admin.getId() == 1L) {
                throw new IllegalArgumentException("Security Block: The primary Super Admin account cannot be deactivated.");
            }

            // Rule 2: Cannot deactivate the currently logged-in account
            String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            if (admin.getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new IllegalArgumentException("Security Block: You cannot deactivate your own account while logged in.");
            }
        }

        admin.setActive(newStatus);
        adminRepository.save(admin);
    }

    // 5. UPDATE OWN PROFILE (Secured)
    public void updateMyProfile(String currentEmail, Admin updatedData) {
        Admin existingAdmin = adminRepository.findByEmail(currentEmail);

        if (existingAdmin == null) {
            throw new IllegalArgumentException("Session expired or user not found.");
        }

        // Check if they are changing to an email that someone else already owns
        if (!existingAdmin.getEmail().equalsIgnoreCase(updatedData.getEmail()) &&
                adminRepository.existsByEmail(updatedData.getEmail())) {
            throw new IllegalArgumentException("This email is already taken by another user.");
        }

        // Only update safe fields (Notice we DO NOT touch Role or Active status here)
        existingAdmin.setFullName(updatedData.getFullName());
        existingAdmin.setEmail(updatedData.getEmail());

        if (updatedData.getPassword() != null && !updatedData.getPassword().trim().isEmpty()) {
            existingAdmin.setPassword(updatedData.getPassword());
        }

        adminRepository.save(existingAdmin);
    }
}