package com.quickbite.payment.service;

import com.quickbite.payment.model.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final List<Payment> paymentHistory = new ArrayList<>();

    public Payment processPayment(String orderId, BigDecimal amount, String paymentMethod) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã đơn hàng không được để trống");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }

        String txId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(txId, orderId, amount, paymentMethod != null ? paymentMethod : "CREDIT_CARD", "SUCCESS");
        paymentHistory.add(payment);
        return payment;
    }

    public List<Payment> getAllPayments() {
        return new ArrayList<>(paymentHistory);
    }
}
