package com.yas.payment.paypal;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class PaymentPaypalApplicationTest {

    @Test
    void mainStartsApplication() {
        String[] args = {};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            PaymentPaypalApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(PaymentPaypalApplication.class, args));
        }
    }
}
