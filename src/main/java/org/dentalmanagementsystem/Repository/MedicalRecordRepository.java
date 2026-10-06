package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
    List<MedicalRecord> findByPatientIdAndDentistIdOrderByCreatedAtDesc(Long patientId, Long dentistId);
}