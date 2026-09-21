package org.dentalmanagementsystem.Controller;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.PaymentRecord;
import org.dentalmanagementsystem.Repository.PaymentRecordRepository;
import org.dentalmanagementsystem.Service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRecordRepository paymentRepository;

    @GetMapping("/{appointmentId}")
    public ResponseEntity<?> getPaymentDetails(@PathVariable Long appointmentId) {
        PaymentRecord payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return ResponseEntity.ok(payment);
    }

    // 1. Bank Transfer Flow: Patient uploads the PDF receipt
    @PostMapping("/{paymentId}/upload-receipt")
    public ResponseEntity<?> uploadBankReceipt(@PathVariable Long paymentId, @RequestParam("file") MultipartFile file) {
        try {
            paymentService.uploadBankReceipt(paymentId, file);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Receipt uploaded for review."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to upload receipt: " + e.getMessage());
        }
    }

    // 2. Online Payment Flow: Generate Hash for Checkout Page
    @GetMapping("/{paymentId}/generate-hash")
    public ResponseEntity<?> getCheckoutHash(@PathVariable Long paymentId) {
        PaymentRecord payment = paymentRepository.findById(paymentId).orElseThrow();

        // Generate a unique order ID for PayHere if it doesn't have one yet
        if (payment.getPayhereOrderId() == null) {
            payment.setPayhereOrderId("AD-" + payment.getId() + "-" + System.currentTimeMillis());
            paymentRepository.save(payment);
        }

        String hash = paymentService.generatePayHereHash(payment.getPayhereOrderId(), payment.getAmount());

        return ResponseEntity.ok(Map.of(
                "orderId", payment.getPayhereOrderId(),
                "hash", hash,
                "amount", payment.getAmount()
        ));
    }

    // 3. Online Payment Flow: Server-to-Server Callback (Webhook) from PayHere
    @PostMapping(value = "/payhere-notify", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> handlePayHereNotify(
            @RequestParam("order_id") String orderId,
            @RequestParam("status_code") String statusCode,
            @RequestParam("md5sig") String md5sig,
            @RequestParam("payhere_amount") String payhereAmount,
            @RequestParam("payhere_currency") String payhereCurrency) {

        try {
            paymentService.processPayHereCallback(orderId, statusCode, md5sig, payhereAmount, payhereCurrency);
            return ResponseEntity.ok().build(); // PayHere just expects a 200 OK
        } catch (Exception e) {
            System.err.println("PayHere Webhook Error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}