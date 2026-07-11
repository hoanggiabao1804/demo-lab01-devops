package com.yas.commonlibrary.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.viewmodel.error.ErrorVm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleNotFoundException_returns404Response() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/backoffice/products/99");
        servletRequest.setServletPath("/backoffice/products/99");

        ResponseEntity<ErrorVm> response = handler.handleNotFoundException(
            new NotFoundException("PRODUCT_NOT_FOUND", 99), new ServletWebRequest(servletRequest));

        assertError(response, HttpStatus.NOT_FOUND, "The product 99 is not found", 0);
    }

    @Test
    void handleBadRequestLikeExceptions_return400Response() throws Exception {
        assertError(handler.handleBadRequestException(new BadRequestException("INVALID_ADJUSTED_QUANTITY"), null),
            HttpStatus.BAD_REQUEST, "Invalid adjusted quantity make a negative quantity", 0);
        assertError(handler.handleWrongEmailFormatException(new WrongEmailFormatException("WRONG_EMAIL_FORMAT", "a"), null),
            HttpStatus.BAD_REQUEST, "Wrong email format for a", 0);
        assertError(handler.handleCreateGuestUserException(new CreateGuestUserException("guest"), null),
            HttpStatus.BAD_REQUEST, "guest", 0);
        assertError(handler.handleStockExistingException(new StockExistingException("SKU_ALREADY_EXISTED_OR_DUPLICATED", "S1"),
            null), HttpStatus.BAD_REQUEST, "Sku S1 is already existed or is duplicated", 0);
        assertError(handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("duplicate key")),
            HttpStatus.BAD_REQUEST, "duplicate key", 0);
        assertError(handler.handleDuplicated(new DuplicatedException("NAME_ALREADY_EXITED", "Name")),
            HttpStatus.BAD_REQUEST, "Request name Name is already existed", 0);
        ResponseEntity<ErrorVm> missingParamResponse =
            handler.handleMissingParams(new MissingServletRequestParameterException("id", "Long"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), missingParamResponse.getStatusCode().value());
        assertNotNull(missingParamResponse.getBody());
        assertNull(missingParamResponse.getBody().fieldErrors());
    }

    @Test
    void handleForbiddenLikeExceptions_return403Response() {
        assertError(handler.handleAccessDeniedException(new AccessDeniedException("ACCESS_DENIED"), null),
            HttpStatus.FORBIDDEN, "ACCESS_DENIED", 0);
        assertError(handler.handleSignInRequired(new SignInRequiredException("SIGN_IN_REQUIRED")),
            HttpStatus.FORBIDDEN, "Authentication required", 0);
        assertError(handler.handleForbidden(new NotFoundException("FORBIDDEN"), null),
            HttpStatus.FORBIDDEN, "You don't have permission to access this page", 0);
    }

    @Test
    void handleConflictAndInternalServerError_returnExpectedResponses() {
        assertError(handler.handleResourceExistedException(new ResourceExistedException("RESOURCE_ALREADY_EXISTED"), null),
            HttpStatus.CONFLICT, "Resource already existed", 0);
        assertError(handler.handleInternalServerErrorException(new InternalServerErrorException("PAYMENT_FAIL_MESSAGE")),
            HttpStatus.INTERNAL_SERVER_ERROR, "Payment failed", 0);
        assertError(handler.handleOtherException(new RuntimeException("unexpected"), null),
            HttpStatus.INTERNAL_SERVER_ERROR, "unexpected", 0);
    }

    @Test
    void handleMethodArgumentNotValid_returnsFieldErrors() throws Exception {
        Method method = ApiExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "sampleRequest");
        bindingResult.addError(new FieldError("sampleRequest", "name", "must not be blank"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorVm> response = handler.handleMethodArgumentNotValid(exception, null);

        assertError(response, HttpStatus.BAD_REQUEST, "Request information is not valid", 1);
        assertEquals("name must not be blank", response.getBody().fieldErrors().get(0));
    }

    @Test
    void handleConstraintViolation_returnsFormattedViolationMessages() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("quantity");
        when(violation.getRootBeanClass()).thenReturn((Class) SampleRequest.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be greater than 0");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorVm> response = handler.handleConstraintViolation(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "Request information is not valid", 1);
        assertEquals(SampleRequest.class.getName() + " quantity: must be greater than 0",
            response.getBody().fieldErrors().get(0));
    }

    @SuppressWarnings("unused")
    private void sampleMethod(String name) {
        // Used only to create MethodParameter for MethodArgumentNotValidException.
    }

    private void assertError(ResponseEntity<ErrorVm> response, HttpStatus expectedStatus, String expectedDetail,
                            int expectedFieldErrors) {
        assertEquals(expectedStatus.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(expectedStatus.toString(), response.getBody().statusCode());
        assertEquals(expectedStatus.getReasonPhrase(), response.getBody().title());
        assertEquals(expectedDetail, response.getBody().detail());
        if (expectedFieldErrors == 0) {
            assertTrue(response.getBody().fieldErrors() == null || response.getBody().fieldErrors().isEmpty());
        } else {
            assertNotNull(response.getBody().fieldErrors());
            assertEquals(expectedFieldErrors, response.getBody().fieldErrors().size());
        }
    }

    private static class SampleRequest {
    }
}
