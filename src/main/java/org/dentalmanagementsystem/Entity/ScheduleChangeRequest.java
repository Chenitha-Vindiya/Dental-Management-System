package org.dentalmanagementsystem.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "schedule_change_requests")
public class ScheduleChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dentist_id", nullable = false)
    private Dentist dentist;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private String adminNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // One Request can have multiple Date Records
    @OneToMany(mappedBy = "scheduleChangeRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleChangeRecord> records = new ArrayList<>();

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }
}