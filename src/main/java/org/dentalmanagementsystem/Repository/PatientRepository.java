package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByEmail(String email);
    Patient findByEmail(String email);
}