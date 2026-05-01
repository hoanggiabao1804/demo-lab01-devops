package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private AbstractCircuitBreakFallbackHandler fallbackHandler;

    @BeforeEach
    void setUp() {
        fallbackHandler = new AbstractCircuitBreakFallbackHandler() {
        };
    }

    @Test
    void handleBodilessFallback_ShouldCallHandleErrorAndRethrow() {
        Throwable testException = new RuntimeException("Test Bodiless Exception");

        assertThatThrownBy(() -> fallbackHandler.handleBodilessFallback(testException))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test Bodiless Exception")
                .isSameAs(testException);
    }

    @Test
    void handleTypedFallback_ShouldCallHandleErrorAndRethrow() {
        Throwable testException = new RuntimeException("Test Typed Exception");

        assertThatThrownBy(() -> fallbackHandler.handleTypedFallback(testException))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test Typed Exception")
                .isSameAs(testException);
    }
}