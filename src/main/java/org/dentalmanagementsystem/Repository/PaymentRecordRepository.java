package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByAppointmentId(Long appointmentId);
    Optional<PaymentRecord> findByPayhereOrderId(String orderId);
    List<PaymentRecord> findByAppointmentPatientIdOrderByCreatedAtDesc(Long patientId);
}