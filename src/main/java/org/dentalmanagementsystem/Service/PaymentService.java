package org.dentalmanagementsystem.Service;

import lombok.RequiredArgsConstructor;
import org.dentalmanagementsystem.Entity.PaymentRecord;
import org.dentalmanagementsystem.Repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRecordRepository paymentRepository;

    // Add these to your application.properties later
    @Value("${payhere.merchant.id:1234567}")
    private String merchantId;

    @Value("${payhere.merchant.secret:YOUR_MERCHANT_SECRET}")
    private String merchantSecret;

    public void uploadBankReceipt(Long paymentId, MultipartFile file) {
        PaymentRecord payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        // Logic to save the PDF file to your server/cloud storage goes here
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        // saveFileToStorage(file, fileName);

        payment.setBankReceiptUrl("/uploads/receipts/" + fileName);
        payment.setStatus("PENDING_TRANSFER_VERIFICATION");
        paymentRepository.save(payment);
    }

    public String generatePayHereHash(String orderId, Double amount) {
        try {
            DecimalFormat df = new DecimalFormat("0.00");
            String formattedAmount = df.format(amount);
            String currency = "LKR";

            // PayHere MD5 Generation Logic
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 1. Hash the Merchant Secret
            md.update(merchantSecret.getBytes());
            byte[] digest1 = md.digest();
            String hashedSecret = String.format("%032x", new BigInteger(1, digest1)).toUpperCase();

            // 2. Concatenate and Hash the final string
            String dataToHash = merchantId + orderId + formattedAmount + currency + hashedSecret;
            md.update(dataToHash.getBytes());
            byte[] digest2 = md.digest();

            return String.format("%032x", new BigInteger(1, digest2)).toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating payment hash", e);
        }
    }

    public void processPayHereCallback(String orderId, String statusCode, String md5sig, String payhereAmount, String payhereCurrency) {
        PaymentRecord payment = paymentRepository.findByPayhereOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Invalid Order ID"));

        // Verify the signature to ensure the callback actually came from PayHere
        String expectedHash = generatePayHereHash(orderId, Double.parseDouble(payhereAmount));

        if (expectedHash.equals(md5sig)) {
            if ("2".equals(statusCode)) { // 2 = Success in PayHere
                payment.setStatus("COMPLETED");
            } else if ("-1".equals(statusCode) || "-2".equals(statusCode)) {
                payment.setStatus("REJECTED");
            }
            paymentRepository.save(payment);
        } else {
            System.err.println("PayHere Signature Verification Failed for Order: " + orderId);
        }
    }
}