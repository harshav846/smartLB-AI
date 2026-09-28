package com.smartlb.authservice.exception;

import com.smartlb.authservice.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    // Helper method for generating a MethodParameter for test
    public void dummyMethod(String dummyParam) {}

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/test");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with 422 Unprocessable Entity and field errors")
    void testHandleMethodArgumentNotValid() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "must be a valid email address"));
        bindingResult.addError(new FieldError("registerRequest", "password", "must be at least 8 characters"));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleMethodArgumentNotValid(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(422, body.getStatus());
        assertEquals("Unprocessable Entity", body.getError());
        assertEquals("VALIDATION_FAILED", body.getErrorCode());
        assertEquals("Request payload validation failed", body.getMessage());
        assertEquals("/api/v1/auth/test", body.getPath());
        assertNotNull(body.getValidationErrors());
        assertEquals("must be a valid email address", body.getValidationErrors().get("email"));
        assertEquals("must be at least 8 characters", body.getValidationErrors().get("password"));
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException with 422 Unprocessable Entity")
    void testHandleConstraintViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("orgSlug");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleConstraintViolation(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(422, body.getStatus());
        assertEquals("VALIDATION_FAILED", body.getErrorCode());
        assertEquals("must not be blank", body.getValidationErrors().get("orgSlug"));
    }

    @Test
    @DisplayName("Should handle BusinessException with 422 Unprocessable Entity")
    void testHandleBusinessException() {
        BusinessException ex = new BusinessException("Domain rule violated");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleBusinessException(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(422, body.getStatus());
        assertEquals("BAD_REQUEST", body.getErrorCode());
        assertEquals("Domain rule violated", body.getMessage());
    }

    @Test
    @DisplayName("Should handle BadRequestException with 400 Bad Request")
    void testHandleBadRequest() {
        BadRequestException ex = new BadRequestException("Invalid parameter combination");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("BAD_REQUEST", body.getErrorCode());
        assertEquals("Invalid parameter combination", body.getMessage());
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with 400 Bad Request and sanitized message")
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error: Unexpected character...");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("MALFORMED_REQUEST_BODY", body.getErrorCode());
        assertEquals("Malformed JSON request body or unparseable payload", body.getMessage());
    }

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException with 400 Bad Request")
    void testHandleMissingServletRequestParameter() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("tenantId", "String");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleMissingServletRequestParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("MISSING_REQUIRED_FIELD", body.getErrorCode());
        assertTrue(body.getMessage().contains("tenantId"));
    }

    @Test
    @DisplayName("Should handle UnauthorizedException with 401 Unauthorized")
    void testHandleUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Authentication token missing");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleUnauthorized(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(401, body.getStatus());
        assertEquals("AUTHENTICATION_REQUIRED", body.getErrorCode());
        assertEquals("Authentication token missing", body.getMessage());
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with 401 Unauthorized")
    void testHandleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Invalid email or password");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleBadCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(401, body.getStatus());
        assertEquals("INVALID_CREDENTIALS", body.getErrorCode());
        assertEquals("Invalid email or password", body.getMessage());
    }

    @Test
    @DisplayName("Should handle AccountStatusException with 403 Forbidden")
    void testHandleAccountStatus() {
        AccountStatusException ex = new AccountStatusException("Account is locked due to excessive failed attempts");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleAccountStatus(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(403, body.getStatus());
        assertEquals("ACCOUNT_LOCKED", body.getErrorCode());
        assertEquals("Account is locked due to excessive failed attempts", body.getMessage());
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException and domain subclasses with 404 Not Found")
    void testHandleResourceNotFound() {
        UserNotFoundException userEx = new UserNotFoundException("User not found with email: test@smartlb.io");

        ResponseEntity<ErrorResponse> userResponse = exceptionHandler.handleResourceNotFound(userEx, request);

        assertEquals(HttpStatus.NOT_FOUND, userResponse.getStatusCode());
        ErrorResponse userBody = userResponse.getBody();
        assertNotNull(userBody);
        assertEquals(404, userBody.getStatus());
        assertEquals("RESOURCE_NOT_FOUND", userBody.getErrorCode());
        assertEquals("User not found with email: test@smartlb.io", userBody.getMessage());

        OrganizationNotFoundException orgEx = new OrganizationNotFoundException("Organization not found with slug: acme");
        ResponseEntity<ErrorResponse> orgResponse = exceptionHandler.handleResourceNotFound(orgEx, request);
        assertEquals(HttpStatus.NOT_FOUND, orgResponse.getStatusCode());
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException with 409 Conflict")
    void testHandleDuplicateResource() {
        DuplicateResourceException ex = new DuplicateResourceException("Organization with slug 'acme-corp' already exists");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleDuplicateResource(ex, request);

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(409, body.getStatus());
        assertEquals("RESOURCE_ALREADY_EXISTS", body.getErrorCode());
        assertEquals("Organization with slug 'acme-corp' already exists", body.getMessage());
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException with 409 Conflict and sanitized message")
    void testHandleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint 'uk_users_email'\n  Detail: Key (email)=(test@smartlb.io) already exists.");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(409, body.getStatus());
        assertEquals("RESOURCE_ALREADY_EXISTS", body.getErrorCode());
        // Verify raw SQL details are NOT exposed to client
        assertFalse(body.getMessage().contains("uk_users_email"));
        assertFalse(body.getMessage().contains("Key (email)"));
        assertEquals("A database constraint violation occurred. A resource with identical details may already exist.", body.getMessage());
    }

    @Test
    @DisplayName("Should handle unhandled generic Exception with 500 Internal Server Error and sanitized response")
    void testHandleGenericException() {
        RuntimeException ex = new NullPointerException("Internal null reference error at class X line Y");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", body.getErrorCode());
        // Verify stacktrace / internal error message is NOT exposed to client
        assertFalse(body.getMessage().contains("NullPointerException"));
        assertFalse(body.getMessage().contains("line Y"));
        assertEquals("An internal server error occurred. Please contact support if the issue persists.", body.getMessage());
    }
}
