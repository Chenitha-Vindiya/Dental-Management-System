package org.dentalmanagementsystem.Service;

import org.dentalmanagementsystem.Entity.Patient;
import org.dentalmanagementsystem.Repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public Patient getPatientByEmail(String email) {
        return patientRepository.findByEmail(email);
    }

    public void updatePatientInfo(Patient patient, String fullName, String phoneNumber) {
        patient.setFullName(fullName);
        patient.setPhoneNumber(phoneNumber);
        patientRepository.save(patient);
    }

    public void updatePassword(Patient patient, String newPassword) {
        patient.setPassword(newPassword);
        patientRepository.save(patient);
    }

    public void deactivateAccount(Patient patient) {
        patient.setActive(false);
        patientRepository.save(patient);
    }
}