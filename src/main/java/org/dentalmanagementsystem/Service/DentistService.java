package org.dentalmanagementsystem.Service;

import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Repository.DentistRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DentistService {

    private final DentistRepository dentistRepository;

    public DentistService(DentistRepository dentistRepository) {
        this.dentistRepository = dentistRepository;
    }

    public List<Dentist> getAllDentists() {
        return dentistRepository.findAll();
    }

    public void createDentist(Dentist dentist) {
        // Strict Backend Validation: Enforce Domain
        if (!dentist.getEmail().toLowerCase().endsWith("@dentist.com")) {
            throw new IllegalArgumentException("Dentist email must use the @dentist.com domain.");
        }

        if (dentistRepository.existsByEmail(dentist.getEmail())) {
            throw new IllegalArgumentException("A dentist with this email already exists.");
        }

        dentist.setActive(true);
        // Default specialization if none provided
        if (dentist.getSpecialization() == null || dentist.getSpecialization().trim().isEmpty()) {
            dentist.setSpecialization("General Dentistry");
        }

        dentistRepository.save(dentist);
    }

    public void toggleDentistStatus(Long id, boolean status) {
        Dentist dentist = dentistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dentist not found."));

        dentist.setActive(status);
        dentistRepository.save(dentist);
    }
}