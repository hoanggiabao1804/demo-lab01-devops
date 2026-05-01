package com.yas.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.yas.media.utils.FileTypeValidator;
import com.yas.media.utils.ValidFileType;

@ExtendWith(MockitoExtension.class)
class FileTypeValidatorTest {

    private FileTypeValidator validator;

    @Mock
    private ValidFileType validFileTypeAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        validator = new FileTypeValidator();

        // Cấu hình annotation giả lập
        when(validFileTypeAnnotation.allowedTypes())
                .thenReturn(new String[] { "image/jpeg", "image/png", "image/gif" });
        when(validFileTypeAnnotation.message()).thenReturn("Invalid file type");

        // Gọi hàm initialize để validator nhận giá trị từ annotation
        validator.initialize(validFileTypeAnnotation);
    }

    /**
     * Hàm tiện ích để giả lập luồng gọi của ConstraintValidatorContext
     * khi file bị từ chối (tránh NullPointerException khi code chạy dòng
     * context.build...)
     */
    private void mockConstraintViolation() {
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Nested
    class NullChecks {
        @Test
        void shouldReturnFalse_WhenFileIsNull() {
            mockConstraintViolation();

            boolean result = validator.isValid(null, context);

            assertThat(result).isFalse();
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Invalid file type");
        }

        @Test
        void shouldReturnFalse_WhenContentTypeIsNull() {
            mockConstraintViolation();
            when(file.getContentType()).thenReturn(null);

            boolean result = validator.isValid(file, context);

            assertThat(result).isFalse();
            verify(context).disableDefaultConstraintViolation();
        }
    }

    @Nested
    class ContentTypeChecks {
        @Test
        void shouldReturnFalse_WhenContentTypeIsNotInAllowedList() {
            mockConstraintViolation();
            when(file.getContentType()).thenReturn("application/pdf"); // Cố tình truyền loại file không cho phép

            boolean result = validator.isValid(file, context);

            assertThat(result).isFalse();
            verify(context).disableDefaultConstraintViolation();
        }
    }

    @Nested
    class ImageIOReadingChecks {
        @Test
        void shouldReturnTrue_WhenFileIsRealImage() throws IOException {
            when(file.getContentType()).thenReturn("image/gif");

            // Dùng Base64 của ảnh GIF 1x1 thật 100% để ImageIO.read() có thể đọc được và
            // trả về đối tượng
            String base64Gif = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
            byte[] realImageBytes = Base64.getDecoder().decode(base64Gif);

            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(realImageBytes));

            boolean result = validator.isValid(file, context);

            // Kiểm tra ImageIO.read trả về khác null và validator xác nhận hợp lệ
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_WhenFileContentIsNotAnImage() throws IOException {
            when(file.getContentType()).thenReturn("image/jpeg");

            // Dù ContentType là image/jpeg nhưng nội dung lại là text linh tinh
            // ImageIO.read() sẽ đọc không ra ảnh và trả về giá trị null
            byte[] fakeImageBytes = "Đây không phải là một bức ảnh".getBytes();
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(fakeImageBytes));

            boolean result = validator.isValid(file, context);

            // Validator phải nhận diện ra file lỗi và trả về false
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_WhenIOExceptionOccurs() throws IOException {
            when(file.getContentType()).thenReturn("image/png");

            // Giả lập ngoại lệ văng ra khi đang đọc file InputStream
            when(file.getInputStream()).thenThrow(new IOException("Stream reading error"));

            boolean result = validator.isValid(file, context);

            // Nhảy vào catch block và trả về false
            assertThat(result).isFalse();
        }
    }
}