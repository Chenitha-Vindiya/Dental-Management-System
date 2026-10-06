package org.dentalmanagementsystem.Service;

import org.dentalmanagementsystem.Entity.*;
import org.dentalmanagementsystem.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DentistScheduleService {

    @Autowired
    private DentistRepository dentistRepository;

    @Autowired
    private ScheduleChangeRequestRepository scheduleChangeRequestRepository;

    @Autowired
    private ScheduleChangeRecordRepository scheduleChangeRecordRepository;

    @Autowired
    private DentistScheduleRepository dentistScheduleRepository;


    // Cancel pending request (Dentist action)
    @Transactional
    public boolean cancelPendingRequest(Long requestId, String email) {
        Dentist dentist = dentistRepository.findByEmail(email);
        if (dentist == null) {
            throw new RuntimeException("Dentist not found");
        }

        ScheduleChangeRequest request = scheduleChangeRequestRepository.findById(requestId).orElse(null);

        // Ensure request exists, belongs to this dentist, and is still PENDING
        if (request != null && request.getDentist().getId().equals(dentist.getId())) {
            if (request.getStatus() == ScheduleChangeRequest.RequestStatus.PENDING) {
                scheduleChangeRequestRepository.delete(request);
                return true;
            }
        }
        return false;
    }

    // Process request (Admin action: Approve / Reject)
    @Transactional
    public void processRequest(Long requestId, ScheduleChangeRequest.RequestStatus newStatus, String adminNotes) {
        ScheduleChangeRequest request = scheduleChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Schedule change request not found with ID: " + requestId));

        if (request.getStatus() != ScheduleChangeRequest.RequestStatus.PENDING) {
            throw new RuntimeException("This request has already been processed.");
        }

        request.setStatus(newStatus);
        if (adminNotes != null && !adminNotes.trim().isEmpty()) {
            request.setAdminNotes(adminNotes);
        }

        // IF APPROVED: Update schedule records accordingly
        if (newStatus == ScheduleChangeRequest.RequestStatus.APPROVED) {
            Long dentistId = request.getDentist().getId();

            for (ScheduleChangeRecord record : request.getRecords()) {

                if (record.getChangeType() == ScheduleChangeRecord.ChangeType.PERMANENT) {
                    // Use explicit dayOfWeek for permanent changes
                    DayOfWeek targetDay = record.getDayOfWeek();
                    if (targetDay == null) {
                        throw new RuntimeException("Permanent schedule record is missing dayOfWeek.");
                    }

                    DentistSchedule weeklySchedule = dentistScheduleRepository
                            .findByDentistId(dentistId)
                            .stream()
                            .filter(s -> s.getDayOfWeek() == targetDay)
                            .findFirst()
                            .orElse(new DentistSchedule());

                    weeklySchedule.setDentist(request.getDentist());
                    weeklySchedule.setDayOfWeek(targetDay);

                    // ROBUST CHECK: Treat as day off if flagged or if both times are null/empty
                    boolean isOffDay = record.isFullDayOff() ||
                            (record.getNewStartTime() == null && record.getNewEndTime() == null);

                    if (isOffDay) {
                        weeklySchedule.setWorkingDay(false); // Force working day to OFF
                        weeklySchedule.setStartTime(null);
                        weeklySchedule.setEndTime(null);
                    } else {
                        weeklySchedule.setWorkingDay(true); // Force working day to ACTIVE
                        weeklySchedule.setStartTime(record.getNewStartTime());
                        weeklySchedule.setEndTime(record.getNewEndTime());
                    }

                    dentistScheduleRepository.save(weeklySchedule);

                } else if (record.getChangeType() == ScheduleChangeRecord.ChangeType.TEMPORARY) {
                    // Temporary override handling using targetDate
                    System.out.println("Approved temporary override for date: " + record.getTargetDate());
                }
            }
        }

        scheduleChangeRequestRepository.save(request);
    }

    public DentistSchedule getEffectiveScheduleForDate(Long dentistId, LocalDate targetDate) {

        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();

        // 1. Get the permanent weekly base schedule
        DentistSchedule baseSchedule = dentistScheduleRepository.findByDentistId(dentistId).stream()
                .filter(s -> s.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(new DentistSchedule());

        // 2. Create a temporary in-memory copy (so we don't accidentally overwrite the DB)
        DentistSchedule effectiveSchedule = new DentistSchedule();
        effectiveSchedule.setDayOfWeek(dayOfWeek);
        effectiveSchedule.setWorkingDay(baseSchedule.isWorkingDay());
        effectiveSchedule.setStartTime(baseSchedule.getStartTime());
        effectiveSchedule.setEndTime(baseSchedule.getEndTime());

        // 3. Check for an APPROVED TEMPORARY override for this specific date
        Optional<ScheduleChangeRecord> tempOverride = scheduleChangeRecordRepository.findApprovedTemporaryOverride(dentistId, targetDate);

        if (tempOverride.isPresent()) {
            ScheduleChangeRecord override = tempOverride.get();

            boolean isOffDay = override.isFullDayOff() ||
                    (override.getNewStartTime() == null && override.getNewEndTime() == null);

            if (isOffDay) {
                effectiveSchedule.setWorkingDay(false);
                effectiveSchedule.setStartTime(null);
                effectiveSchedule.setEndTime(null);
            } else {
                effectiveSchedule.setWorkingDay(true);
                effectiveSchedule.setStartTime(override.getNewStartTime());
                effectiveSchedule.setEndTime(override.getNewEndTime());
            }
        }

        return effectiveSchedule;
    }
}