package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenCodeExists_formatsMessageWithArguments() {
        assertEquals("The product 15 is not found", MessagesUtils.getMessage("PRODUCT_NOT_FOUND", 15));
    }

    @Test
    void getMessage_whenCodeDoesNotExist_returnsCodeItself() {
        assertEquals("NOT_DEFINED_CODE", MessagesUtils.getMessage("NOT_DEFINED_CODE"));
    }

    @Test
    void format_usesDefaultAndCustomPatterns() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 22, 9, 8, 7);

        assertEquals("22-04-2026_09-08-07", DateTimeUtils.format(dateTime));
        assertEquals("2026/04/22 09:08", DateTimeUtils.format(dateTime, "yyyy/MM/dd HH:mm"));
    }
}
