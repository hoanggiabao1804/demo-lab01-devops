package com.yas.payment.service.provider.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient; // IMPORT THÊM DÒNG NÀY
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaypalHandlerTest {

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaypalService paypalService;

    private PaypalHandler paypalHandler;

    @BeforeEach
    void setUp() {
        // Dùng spy để bọc object thật, cho phép mock các hàm kế thừa từ class cha
        // (AbstractPaymentHandler)
        paypalHandler = spy(new PaypalHandler(paymentProviderService, paypalService));

        // Thêm lenient() để Mockito không báo lỗi UnnecessaryStubbing khi test hàm
        // getProviderId
        lenient().doReturn(null).when(paypalHandler).getPaymentSettings(anyString());
    }

    @Test
    void getProviderId_ShouldReturnPaypalName() {
        String providerId = paypalHandler.getProviderId();
        assertThat(providerId).isEqualTo(PaymentMethod.PAYPAL.name());
    }

    @Test
    void initPayment_ShouldMapRequestAndReturnInitiatedPayment() {
        // 1. Chuẩn bị dữ liệu đầu vào
        InitPaymentRequestVm requestVm = mock(InitPaymentRequestVm.class);
        when(requestVm.totalPrice()).thenReturn(BigDecimal.valueOf(150.00));
        when(requestVm.checkoutId()).thenReturn("CHK-12345");
        when(requestVm.paymentMethod()).thenReturn("PAYPAL");

        // 2. Giả lập PaypalService trả về response
        PaypalCreatePaymentResponse mockResponse = mock(PaypalCreatePaymentResponse.class);
        when(mockResponse.status()).thenReturn("CREATED");
        when(mockResponse.paymentId()).thenReturn("PAY-999");
        when(mockResponse.redirectUrl()).thenReturn("https://sandbox.paypal.com/checkout");

        when(paypalService.createPayment(any(PaypalCreatePaymentRequest.class))).thenReturn(mockResponse);

        // 3. Gọi hàm cần test
        InitiatedPayment result = paypalHandler.initPayment(requestVm);

        // 4. Kiểm chứng dữ liệu gọi sang PaypalService (Đảm bảo mapping đúng field)
        ArgumentCaptor<PaypalCreatePaymentRequest> reqCaptor = ArgumentCaptor
                .forClass(PaypalCreatePaymentRequest.class);
        verify(paypalService).createPayment(reqCaptor.capture());
        PaypalCreatePaymentRequest capturedReq = reqCaptor.getValue();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getPaymentId()).isEqualTo("PAY-999");
        assertThat(result.getRedirectUrl()).isEqualTo("https://sandbox.paypal.com/checkout");
    }

    @Test
    void capturePayment_ShouldMapRequestAndReturnCapturedPayment() {
        // 1. Chuẩn bị dữ liệu đầu vào (Sử dụng Record CapturePaymentRequestVm)
        CapturePaymentRequestVm requestVm = new CapturePaymentRequestVm("PAYPAL", "TOKEN-ABC-123");

        // 2. Giả lập PaypalService trả về response
        PaypalCapturePaymentResponse mockResponse = mock(PaypalCapturePaymentResponse.class);
        when(mockResponse.checkoutId()).thenReturn("CHK-12345");
        when(mockResponse.amount()).thenReturn(BigDecimal.valueOf(150.00));
        when(mockResponse.paymentFee()).thenReturn(BigDecimal.valueOf(4.50));
        when(mockResponse.gatewayTransactionId()).thenReturn("TRANS-777");
        when(mockResponse.paymentMethod()).thenReturn("PAYPAL");
        when(mockResponse.paymentStatus()).thenReturn("COMPLETED");
        when(mockResponse.failureMessage()).thenReturn(null);

        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class))).thenReturn(mockResponse);

        // 3. Gọi hàm cần test
        CapturedPayment result = paypalHandler.capturePayment(requestVm);

        // 4. Kiểm chứng kết quả (Assert)
        ArgumentCaptor<PaypalCapturePaymentRequest> reqCaptor = ArgumentCaptor
                .forClass(PaypalCapturePaymentRequest.class);
        verify(paypalService).capturePayment(reqCaptor.capture());

        assertThat(result).isNotNull();
        assertThat(result.getCheckoutId()).isEqualTo("CHK-12345");
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(150.00));
        assertThat(result.getPaymentFee()).isEqualTo(BigDecimal.valueOf(4.50));
        assertThat(result.getGatewayTransactionId()).isEqualTo("TRANS-777");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getFailureMessage()).isNull();
    }
}