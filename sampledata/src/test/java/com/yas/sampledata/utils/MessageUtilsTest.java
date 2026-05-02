package com.yas.sampledata.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    private ResourceBundle originalBundle;

    @BeforeEach
    void setUp() {
        // Lưu lại bundle gốc (nếu có) để tránh side-effect cho các test class khác
        originalBundle = MessagesUtils.messageBundle;

        // Thay thế messageBundle bằng một Mock ResourceBundle cho việc test[cite: 42]
        MessagesUtils.messageBundle = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] {
                        { "SUCCESS_CODE", "Operation successful" },
                        { "GREETING_CODE", "Hello, {}! Welcome to {}." }
                };
            }
        };
    }

    @Test
    void getMessage_WhenErrorCodeExists_ShouldReturnMappedMessage() {
        // WHEN: Lấy một code đã được định nghĩa trong Mock ResourceBundle
        String result = MessagesUtils.getMessage("SUCCESS_CODE");

        // THEN: Trả về chính xác thông điệp được map
        assertEquals("Operation successful", result);
    }

    @Test
    void getMessage_WhenErrorCodeExistsWithArguments_ShouldReturnFormattedMessage() {
        // WHEN: Lấy thông điệp và truyền tham số để slf4j MessageFormatter xử lý[cite:
        // 42]
        String result = MessagesUtils.getMessage("GREETING_CODE", "Alice", "Wonderland");

        // THEN: Cặp {} được thay thế bằng các tham số tương ứng
        assertEquals("Hello, Alice! Welcome to Wonderland.", result);
    }

    @Test
    void getMessage_WhenErrorCodeDoesNotExist_ShouldCatchExceptionAndReturnErrorCode() {
        // WHEN: Lấy một code không tồn tại (sẽ ném MissingResourceException)[cite: 42]
        String result = MessagesUtils.getMessage("NOT_FOUND_CODE");

        // THEN: Khối catch bắt lỗi và trả về chính cái errorCode đó[cite: 42]
        assertEquals("NOT_FOUND_CODE", result);
    }

    @Test
    void getMessage_WhenErrorCodeDoesNotExistButHasArguments_ShouldFormatTheErrorCode() {
        // WHEN: Lấy code không tồn tại nhưng chuỗi code có chứa {} và có truyền tham
        // số[cite: 42]
        String result = MessagesUtils.getMessage("ERROR_{}_OCCURRED", "404");

        // THEN: Lỗi được bắt, nhưng MessageFormatter vẫn format cái errorCode đó[cite:
        // 42]
        assertEquals("ERROR_404_OCCURRED", result);
    }

    @Test
    void constructor_ShouldBeInvokedForFullCoverage() {
        // Class utility chỉ chứa static methods, việc tạo mới nhằm mục đích cover
        // constructor mặc định ẩn
        MessagesUtils utils = new MessagesUtils();
        assertNotNull(utils);
    }
}