package org.dentalmanagementsystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_records")
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String method; // CASH, BANK_TRANSFER, ONLINE

    @Column(nullable = false)
    private String status; // PENDING_VISIT, PENDING_TRANSFER, PENDING_ONLINE, COMPLETED, REJECTED

    // For Bank Transfers: Stores the path/URL to the uploaded PDF receipt
    private String bankReceiptUrl;

    // For PayHere Online Payments: Unique identifier sent to the gateway
    @Column(unique = true)
    private String payhereOrderId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}