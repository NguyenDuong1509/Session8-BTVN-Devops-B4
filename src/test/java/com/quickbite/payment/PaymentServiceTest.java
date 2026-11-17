package com.quickbite.payment;

import com.quickbite.payment.model.Payment;
import com.quickbite.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    @Test
    @DisplayName("Unit Test 1: Xử lý thanh toán thành công khi dữ liệu hợp lệ")
    void testProcessPayment_Success() {
        String orderId = "ORD-123456";
        BigDecimal amount = new BigDecimal("250000.00");
        String method = "MOMO_WALLET";

        Payment payment = paymentService.processPayment(orderId, amount, method);

        assertNotNull(payment);
        assertNotNull(payment.getTransactionId());
        assertTrue(payment.getTransactionId().startsWith("PAY-"));
        assertEquals("ORD-123456", payment.getOrderId());
        assertEquals(new BigDecimal("250000.00"), payment.getAmount());
        assertEquals("SUCCESS", payment.getStatus());
    }

    @Test
    @DisplayName("Unit Test 2: Ném ngoại lệ khi số tiền thanh toán <= 0")
    void testProcessPayment_InvalidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPayment("ORD-123", BigDecimal.ZERO, "CASH")
        );
    }

    @Test
    @DisplayName("Unit Test 3: Ném ngoại lệ khi orderId để trống")
    void testProcessPayment_EmptyOrderId() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPayment("", new BigDecimal("100000.00"), "CREDIT_CARD")
        );
    }
}
