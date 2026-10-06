package org.dentalmanagementsystem.Controller;

import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Entity.DentistSchedule;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    // Submit Schedule Change Request (Dentist)
    @PostMapping("/api/dentist/schedule/request-change")
    public ResponseEntity<?> requestScheduleChange(@RequestBody ScheduleChangeRequest request, Principal principal) {
        Dentist dentist = dentistRepository.findByEmail(principal.getName());
        if (dentist == null) return ResponseEntity.badRequest().body("Dentist not found");

        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            return ResponseEntity.badRequest().body("At least one schedule record is required.");
        }

        Set<LocalDate> uniqueDates = new HashSet<>();
        Set<DayOfWeek> uniqueDays = new HashSet<>();

        for (ScheduleChangeRecord record : request.getRecords()) {
            if (record.getChangeType() == ScheduleChangeRecord.ChangeType.TEMPORARY) {
                if (record.getTargetDate() == null) {
                    return ResponseEntity.badRequest().body("Target date cannot be empty for temporary requests.");
                }
                if (!uniqueDates.add(record.getTargetDate())) {
                    return ResponseEntity.badRequest().body("Duplicate target date found in request: " + record.getTargetDate());
                }
                boolean alreadyPending = scheduleChangeRecordRepository.existsPendingByDentistAndDate(dentist.getId(), record.getTargetDate());
                if (alreadyPending) {
                    return ResponseEntity.badRequest().body("You already have a pending request for the date: " + record.getTargetDate());
                }
                record.setDayOfWeek(null);
            } else if (record.getChangeType() == ScheduleChangeRecord.ChangeType.PERMANENT) {
                if (record.getDayOfWeek() == null) {
                    return ResponseEntity.badRequest().body("Day of week cannot be empty for permanent requests.");
                }
                if (!uniqueDays.add(record.getDayOfWeek())) {
                    return ResponseEntity.badRequest().body("Duplicate day of week found in request: " + record.getDayOfWeek());
                }
                record.setTargetDate(null);
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

    // Cancel Pending Schedule Change Request (Dentist)
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

    // Approve Schedule Request (Admin)
    @PostMapping("/api/admin/schedule/requests/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveRequest(@PathVariable Long id, @RequestBody(required = false) Map<String, String> payload) {
        try {
            String adminNotes = payload != null ? payload.get("adminNotes") : null;
            dentistScheduleService.processRequest(id, ScheduleChangeRequest.RequestStatus.APPROVED, adminNotes);
            return ResponseEntity.ok().body("Request approved successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to approve request: " + e.getMessage());
        }
    }

    // Reject Schedule Request (Admin)
    @PostMapping("/api/admin/schedule/requests/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody(required = false) Map<String, String> payload) {
        try {
            String adminNotes = payload != null ? payload.get("adminNotes") : null;
            dentistScheduleService.processRequest(id, ScheduleChangeRequest.RequestStatus.REJECTED, adminNotes);
            return ResponseEntity.ok().body("Request rejected.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to reject request: " + e.getMessage());
        }
    }

    @GetMapping("/api/dentist/schedule/week-data")
    @ResponseBody
    public ResponseEntity<?> getWeeklyScheduleData(@RequestParam("startDate") String startDateStr, Principal principal) {
        Dentist dentist = dentistRepository.findByEmail(principal.getName());
        if (dentist == null) return ResponseEntity.badRequest().build();

        LocalDate startDate = LocalDate.parse(startDateStr);
        java.util.List<Map<String, Object>> weekData = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");

        // Fetch temporary change/leave records for this dentist to cross-reference the week
        LocalDate endDate = startDate.plusDays(6);
        java.util.List<ScheduleChangeRecord> changeRecords = scheduleChangeRecordRepository.findByDentistIdAndTargetDateBetween(dentist.getId(), startDate, endDate);

        Set<LocalDate> tempOffDates = new java.util.HashSet<>();
        for (ScheduleChangeRecord record : changeRecords) {
            if (record.getTargetDate() != null &&
                    record.getChangeType() == ScheduleChangeRecord.ChangeType.TEMPORARY &&
                    (record.isFullDayOff() || record.getNewStartTime() == null)) {
                tempOffDates.add(record.getTargetDate());
            }
        }

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            DentistSchedule schedule = dentistScheduleService.getEffectiveScheduleForDate(dentist.getId(), currentDate);

            Map<String, Object> dayData = new java.util.HashMap<>();
            dayData.put("date", currentDate.toString());

            String displayDate = currentDate.getDayOfWeek().toString().substring(0, 1) +
                    currentDate.getDayOfWeek().toString().substring(1).toLowerCase() +
                    ", " + currentDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
            dayData.put("displayDate", displayDate);

            // Check if it's a temporary leave day
            boolean isTempOff = tempOffDates.contains(currentDate);
            dayData.put("isTempOff", isTempOff);
            dayData.put("isWorkingDay", !isTempOff && schedule.isWorkingDay());

            if (!isTempOff && schedule.isWorkingDay() && schedule.getStartTime() != null && schedule.getEndTime() != null) {
                dayData.put("timeRange", schedule.getStartTime().format(timeFormatter) + " - " + schedule.getEndTime().format(timeFormatter));
            }
            weekData.add(dayData);
        }
        return ResponseEntity.ok(weekData);
    }

    @GetMapping("/api/admin/dentists/{id}/schedule")
    @ResponseBody
    public ResponseEntity<?> getDentistMonthlySchedule(@PathVariable Long id, @RequestParam int year, @RequestParam int month) {
        try {
            LocalDate startOfMonth = LocalDate.of(year, month, 1);
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

            // Fetch temporary change/leave records for this dentist in this month range
            java.util.List<ScheduleChangeRecord> changeRecords = scheduleChangeRecordRepository.findByDentistIdAndTargetDateBetween(id, startOfMonth, endOfMonth);

            // Collect dates that have a temporary full-day off
            Set<LocalDate> tempOffDates = new java.util.HashSet<>();
            for (ScheduleChangeRecord record : changeRecords) {
                if (record.getTargetDate() != null &&
                        record.getChangeType() == ScheduleChangeRecord.ChangeType.TEMPORARY &&
                        (record.isFullDayOff() || record.getNewStartTime() == null)) {
                    tempOffDates.add(record.getTargetDate());
                }
            }

            Map<String, String> dateStatuses = new java.util.HashMap<>();
            LocalDate current = startOfMonth;

            while (!current.isAfter(endOfMonth)) {
                // If it exists in the temporary leave table, mark as TEMP_OFF
                if (tempOffDates.contains(current)) {
                    dateStatuses.put(current.toString(), "TEMP_OFF");
                } else {
                    // Otherwise evaluate standard effective schedule (Working vs Off)
                    DentistSchedule schedule = dentistScheduleService.getEffectiveScheduleForDate(id, current);

                    if (schedule == null || !schedule.isWorkingDay()) {
                        dateStatuses.put(current.toString(), "OFF");
                    } else {
                        dateStatuses.put(current.toString(), "WORKING");
                    }
                }
                current = current.plusDays(1);
            }

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("year", year);
            response.put("month", month);
            response.put("scheduleMap", dateStatuses);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching schedule: " + e.getMessage());
        }
    }
}