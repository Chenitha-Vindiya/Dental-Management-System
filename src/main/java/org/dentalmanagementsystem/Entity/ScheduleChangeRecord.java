package org.dentalmanagementsystem.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "schedule_change_records")
public class ScheduleChangeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ScheduleChangeRequest scheduleChangeRequest;

    @Enumerated(EnumType.STRING)
    private ChangeType changeType; // TEMPORARY or PERMANENT

    private LocalDate targetDate; // Used for TEMPORARY

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek; // NEW: Used for PERMANENT

    private boolean isFullDayOff;

    private LocalTime newStartTime;

    private LocalTime newEndTime;

    public enum ChangeType {
        TEMPORARY, PERMANENT
    }
}