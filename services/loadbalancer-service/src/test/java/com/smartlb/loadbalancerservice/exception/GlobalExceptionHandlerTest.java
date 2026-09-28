package com.smartlb.loadbalancerservice.exception;

import com.smartlb.loadbalancerservice.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    public void sampleMethod(String arg) {}

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/instances");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404 Not Found")
    void testResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Instance not found");
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Instance not found", response.getBody().getMessage());
        assertEquals("/api/instances", response.getBody().getPath());
    }

    @Test
    @DisplayName("Should handle BadRequestException with 400 Bad Request")
    void testBadRequest() {
        BadRequestException ex = new BadRequestException("Invalid port configuration");
        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid port configuration", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException with 409 Conflict")
    void testDuplicateResource() {
        DuplicateResourceException ex = new DuplicateResourceException("Instance already exists");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResourceException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Instance already exists", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle ForbiddenException with 403 Forbidden")
    void testForbiddenException() {
        ForbiddenException ex = new ForbiddenException("Cannot modify this instance");
        ResponseEntity<ErrorResponse> response = handler.handleForbiddenException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Cannot modify this instance", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle InvalidTokenException with 401 Unauthorized")
    void testInvalidToken() {
        InvalidTokenException ex = new InvalidTokenException("Token has expired");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidTokenException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Token has expired", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with 403 Forbidden")
    void testAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("You do not have permission to access this resource", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with 400 Bad Request and validation errors map")
    void testMethodArgumentNotValid() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "Instance name is required"));
        bindingResult.addError(new FieldError("request", "port", "Port must be greater than 0"));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", String.class);
        MethodParameter param = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Validation Error", response.getBody().getError());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("Instance name is required", response.getBody().getValidationErrors().get("name"));
        assertEquals("Port must be greater than 0", response.getBody().getValidationErrors().get("port"));
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 Internal Server Error")
    void testGenericException() {
        Exception ex = new RuntimeException("Unexpected database failure");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("An unexpected internal error occurred", response.getBody().getMessage());
    }
}
