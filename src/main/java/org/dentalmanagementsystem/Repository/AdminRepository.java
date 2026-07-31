package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Admin findByEmail(String email);

    boolean existsByEmail(String email);
}