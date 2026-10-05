package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Entity.ScheduleChangeRecord;
import org.dentalmanagementsystem.Entity.ScheduleChangeRequest;
import org.dentalmanagementsystem.Repository.DentistRepository;
import org.dentalmanagementsystem.Repository.ScheduleChangeRecordRepository;
import org.dentalmanagementsystem.Repository.ScheduleChangeRequestRepository;
import org.dentalmanagementsystem.Service.DentistScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class DentistScheduleController {

    @Autowired
    private DentistRepository dentistRepository;

    @Autowired
    private ScheduleChangeRequestRepository scheduleChangeRequestRepository;

    @Autowired
    private ScheduleChangeRecordRepository scheduleChangeRecordRepository;

    @Autowired
    private DentistScheduleService dentistScheduleService;

    // Submit Schedule Change Request
    @PostMapping("/api/dentist/schedule/request-change")
    public ResponseEntity<?> requestScheduleChange(@RequestBody ScheduleChangeRequest request, Principal principal) {
        Dentist dentist = dentistRepository.findByEmail(principal.getName());
        if (dentist == null) return ResponseEntity.badRequest().body("Dentist not found");

        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            return ResponseEntity.badRequest().body("At least one date record is required.");
        }

        // Check for duplicate dates within the incoming request itself, or against existing PENDING records
        java.util.Set<LocalDate> uniqueDates = new java.util.HashSet<>();
        for (ScheduleChangeRecord record : request.getRecords()) {
            if (record.getTargetDate() == null) {
                return ResponseEntity.badRequest().body("Target date cannot be empty.");
            }

            // Check internal duplicates within the payload
            if (!uniqueDates.add(record.getTargetDate())) {
                return ResponseEntity.badRequest().body("Duplicate target date found in request: " + record.getTargetDate() + ". Each record must have a distinct date.");
            }

            // Check if a pending request for this date already exists in the database
            boolean alreadyPending = scheduleChangeRecordRepository.existsPendingByDentistAndDate(dentist.getId(), record.getTargetDate());
            if (alreadyPending) {
                return ResponseEntity.badRequest().body("You already have a pending request for the date: " + record.getTargetDate());
            }

            record.setScheduleChangeRequest(request);
            if (record.isFullDayOff()) {
                record.setNewStartTime(null);
                record.setNewEndTime(null);
            }
        }

        request.setDentist(dentist);
        request.setStatus(ScheduleChangeRequest.RequestStatus.PENDING);

        scheduleChangeRequestRepository.save(request);
        return ResponseEntity.ok("Request submitted successfully");
    }

    // Cancel Pending Schedule Change Request (Linked to the functional Cancel button)
    @DeleteMapping("/api/dentist/schedule/requests/{id}")
    @ResponseBody
    public ResponseEntity<?> cancelScheduleRequest(@PathVariable Long id, Principal principal) {
        try {
            boolean cancelled = dentistScheduleService.cancelPendingRequest(id, principal.getName());
            if (cancelled) {
                return ResponseEntity.ok().body("Request cancelled successfully.");
            } else {
                return ResponseEntity.badRequest().body("Could not cancel request. It may no longer be pending or does not belong to you.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error cancelling request: " + e.getMessage());
        }
    }
}