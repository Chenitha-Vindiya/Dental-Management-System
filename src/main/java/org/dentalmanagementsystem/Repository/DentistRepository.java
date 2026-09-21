package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DentistRepository extends JpaRepository<Dentist, Long> {
    List<Dentist> findByActiveTrue();
    boolean existsByEmail(String email);
    Dentist findByEmail(String email);
}