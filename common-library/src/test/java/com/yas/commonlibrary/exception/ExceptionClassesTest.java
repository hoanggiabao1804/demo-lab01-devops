package com.yas.commonlibrary.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ExceptionClassesTest {

    @Test
    void simpleRuntimeExceptions_keepOriginalMessagesAndCauses() {
        Throwable cause = new IllegalStateException("root cause");

        AccessDeniedException accessDeniedException = new AccessDeniedException("denied");
        assertEquals("denied", accessDeniedException.getMessage());

        CreateGuestUserException createGuestUserException = new CreateGuestUserException("guest error");
        assertEquals("guest error", createGuestUserException.getMessage());

        MultipartFileContentException emptyMultipartException = new MultipartFileContentException();
        assertNull(emptyMultipartException.getMessage());

        MultipartFileContentException multipartMessageException = new MultipartFileContentException("invalid file");
        assertEquals("invalid file", multipartMessageException.getMessage());

        MultipartFileContentException multipartCauseException = new MultipartFileContentException(cause);
        assertSame(cause, multipartCauseException.getCause());

        MultipartFileContentException multipartMessageAndCauseException =
            new MultipartFileContentException("invalid file", cause);
        assertEquals("invalid file", multipartMessageAndCauseException.getMessage());
        assertSame(cause, multipartMessageAndCauseException.getCause());

        UnsupportedMediaTypeException emptyUnsupportedMediaTypeException = new UnsupportedMediaTypeException();
        assertNull(emptyUnsupportedMediaTypeException.getMessage());

        UnsupportedMediaTypeException unsupportedMediaTypeException = new UnsupportedMediaTypeException("image type");
        assertEquals("image type", unsupportedMediaTypeException.getMessage());

        UnsupportedMediaTypeException unsupportedMediaTypeCauseException = new UnsupportedMediaTypeException(cause);
        assertSame(cause, unsupportedMediaTypeCauseException.getCause());

        UnsupportedMediaTypeException unsupportedMediaTypeMessageAndCauseException =
            new UnsupportedMediaTypeException("image type", cause);
        assertEquals("image type", unsupportedMediaTypeMessageAndCauseException.getMessage());
        assertSame(cause, unsupportedMediaTypeMessageAndCauseException.getCause());
    }

    @Test
    void messageCodeBasedExceptions_resolveMessageFromBundle() {
        assertEquals("The product 10 is not found", new BadRequestException("PRODUCT_NOT_FOUND", 10).getMessage());
        assertEquals("Request name Product is already existed", new DuplicatedException("NAME_ALREADY_EXITED", "Product")
            .getMessage());
        assertEquals("You don't have permission to access this page", new ForbiddenException("FORBIDDEN").getMessage());
        assertEquals("Payment failed", new InternalServerErrorException("PAYMENT_FAIL_MESSAGE").getMessage());
        assertEquals("The warehouse 3 is not found", new NotFoundException("WAREHOUSE_NOT_FOUND", 3).getMessage());
        assertEquals("Authentication required", new SignInRequiredException("SIGN_IN_REQUIRED").getMessage());
        assertEquals("Resource already existed", new ResourceExistedException("RESOURCE_ALREADY_EXISTED").getMessage());
        assertEquals("Sku ABC is already existed or is duplicated",
            new StockExistingException("SKU_ALREADY_EXISTED_OR_DUPLICATED", "ABC").getMessage());
        assertEquals("Wrong email format for admin", new WrongEmailFormatException("WRONG_EMAIL_FORMAT", "admin")
            .getMessage());
    }

    @Test
    void mutableMessageExceptions_allowOverridingMessage() {
        Forbidden forbidden = new Forbidden("FORBIDDEN");
        assertEquals("You don't have permission to access this page", forbidden.getMessage());
        forbidden.setMessage("custom forbidden");
        assertEquals("custom forbidden", forbidden.getMessage());

        ResourceExistedException resourceExistedException = new ResourceExistedException("RESOURCE_ALREADY_EXISTED");
        resourceExistedException.setMessage("custom conflict");
        assertEquals("custom conflict", resourceExistedException.getMessage());

        SignInRequiredException signInRequiredException = new SignInRequiredException("SIGN_IN_REQUIRED");
        signInRequiredException.setMessage("custom sign in");
        assertEquals("custom sign in", signInRequiredException.getMessage());
    }

    @Test
    void unknownMessageCode_fallsBackToOriginalCode() {
        assertEquals("UNKNOWN_CODE", new BadRequestException("UNKNOWN_CODE").getMessage());
    }
}
