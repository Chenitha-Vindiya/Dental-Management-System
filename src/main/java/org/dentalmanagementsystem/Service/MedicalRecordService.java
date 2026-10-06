package org.dentalmanagementsystem.Service;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.Appointment;
import org.dentalmanagementsystem.Entity.MedicalRecord;
import org.dentalmanagementsystem.Entity.MedicalRecord.RecordType;
import org.dentalmanagementsystem.Repository.AppointmentRepository;
import org.dentalmanagementsystem.Repository.MedicalRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/medical-records/";

    public MedicalRecord createRecord(Long appointmentId, String type, String notes, MultipartFile file) throws IOException {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setPatient(appointment.getPatient());
        record.setDentist(appointment.getDentist());
        record.setRecordType(RecordType.valueOf(type));
        record.setNotes(notes);

        if (file != null && !file.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFilename = file.getOriginalFilename();
                String fileExtension = "";

                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
                Path filePath = uploadPath.resolve(uniqueFilename);

                // Copy file to target location
                Files.copy(file.getInputStream(), filePath);

                record.setFileName(originalFilename);
                record.setFileUrl("/uploads/medical-records/" + uniqueFilename);
            } catch (Exception e) {
                e.printStackTrace(); // Prints exact error in your IDE console
                throw new RuntimeException("Error saving uploaded file: " + e.getMessage());
            }
        }

        return medicalRecordRepository.save(record);
    }

    public MedicalRecord updateRecordNotes(Long recordId, String newNotes) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        record.setNotes(newNotes);
        return medicalRecordRepository.save(record);
    }

    public MedicalRecord updateRecordWithFile(Long recordId, String newNotes, MultipartFile file) throws IOException {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        if (newNotes != null) {
            record.setNotes(newNotes);
        }

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            record.setFileName(originalFilename);
            record.setFileUrl("/uploads/medical-records/" + uniqueFilename);
        }

        return medicalRecordRepository.save(record);
    }

    public void deleteRecord(Long recordId) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        medicalRecordRepository.delete(record);
    }
}