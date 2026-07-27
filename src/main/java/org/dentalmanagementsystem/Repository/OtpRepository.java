package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {
    // Finds the most recently generated OTP for an email
    OtpRecord findTopByEmailOrderByExpirationTimeDesc(String email);
}