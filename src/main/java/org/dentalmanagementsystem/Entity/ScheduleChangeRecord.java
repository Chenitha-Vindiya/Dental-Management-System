package org.dentalmanagementsystem.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @JsonIgnore // Prevents infinite loops when converting to JSON
    private ScheduleChangeRequest scheduleChangeRequest;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType; // TEMPORARY or PERMANENT

    private boolean isFullDayOff;

    private LocalTime newStartTime;

    private LocalTime newEndTime;

    public enum ChangeType {
        TEMPORARY, PERMANENT
    }
}