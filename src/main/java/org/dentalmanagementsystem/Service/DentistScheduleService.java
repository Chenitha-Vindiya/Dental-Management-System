package org.dentalmanagementsystem.Service;

import org.dentalmanagementsystem.Entity.*;
import org.dentalmanagementsystem.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DentistScheduleService {

    @Autowired
    private DentistRepository dentistRepository;

    @Autowired
    private ScheduleChangeRequestRepository scheduleChangeRequestRepository;

    @Transactional
    public boolean cancelPendingRequest(Long requestId, String email) {
        // Find dentist by email (since email is the unique identifier / username in your Dentist entity)
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
}