package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    // Tạo một class cụ thể kế thừa từ abstract class để có thể test
    private static class TestFallbackHandler extends AbstractCircuitBreakFallbackHandler {
        // Expose các protected methods ra thành public để dễ gọi trong test
        public void testBodilessFallback(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }

        public <T> T testTypedFallback(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }
    }

    private TestFallbackHandler fallbackHandler;

    @BeforeEach
    void setUp() {
        fallbackHandler = new TestFallbackHandler();
    }

    @Test
    void handleBodilessFallback_ShouldLogAndRethrowException() {
        // Arrange
        String errorMessage = "Test Bodiless Exception";
        Throwable testException = new RuntimeException(errorMessage);

        // Act & Assert
        assertThatThrownBy(() -> fallbackHandler.testBodilessFallback(testException))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage)
                .isSameAs(testException); // Đảm bảo ném ra đúng object lỗi ban đầu
    }

    @Test
    void handleTypedFallback_ShouldLogAndRethrowException() {
        // Arrange
        String errorMessage = "Test Typed Exception";
        Throwable testException = new RuntimeException(errorMessage);

        // Act & Assert
        assertThatThrownBy(() -> fallbackHandler.testTypedFallback(testException))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage)
                .isSameAs(testException);
    }
}