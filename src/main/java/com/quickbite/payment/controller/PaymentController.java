package com.quickbite.payment.controller;

import com.quickbite.payment.model.Payment;
import com.quickbite.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@RequestBody Map<String, Object> payload) {
        String orderId = (String) payload.get("orderId");
        Number amountNum = (Number) payload.get("amount");
        BigDecimal amount = amountNum != null ? new BigDecimal(amountNum.toString()) : BigDecimal.ZERO;
        String method = (String) payload.get("paymentMethod");

        Payment payment = paymentService.processPayment(orderId, amount, method);
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
