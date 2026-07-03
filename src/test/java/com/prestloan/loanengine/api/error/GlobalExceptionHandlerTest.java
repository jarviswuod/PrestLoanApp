package com.prestloan.loanengine.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private HttpServletRequest request() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/loans");
    return request;
  }

  @Test
  void shouldHandleNotFound() {
    ResponseEntity<ProblemDetail> response =
        handler.handleNotFound(new NotFoundException("missing loan"), request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).isEqualTo("missing loan");
    assertThat(response.getBody().getProperties()).containsEntry("code", "resource-not-found");
    assertThat(response.getBody().getProperties()).containsKey("errors");
  }

  @Test
  void shouldHandleBadRequest() {
    ResponseEntity<ProblemDetail> response =
        handler.handleBadRequest(new BadRequestException("bad input"), request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).isEqualTo("bad input");
    assertThat(response.getBody().getProperties())
        .containsEntry("code", "business-validation-failed");
  }

  @Test
  void shouldHandleConstraintViolation() {
    @SuppressWarnings("unchecked")
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    when(violation.getMessage()).thenReturn("size must be between 1 and 200");

    ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

    ResponseEntity<ProblemDetail> response = handler.handleConstraintViolation(ex, request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).isEqualTo("Request validation failed");
    assertThat(response.getBody().getProperties())
        .containsEntry("code", "request-validation-failed");
    assertThat(String.valueOf(response.getBody().getProperties().get("errors")))
        .contains("size must be between 1 and 200");
  }

  @Test
  void shouldHandleValidationErrors() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult())
        .thenReturn(
            new org.springframework.validation.BeanPropertyBindingResult(new Object(), "obj"));

    ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValid(ex, request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).isEqualTo("Request validation failed");
    assertThat(response.getBody().getProperties())
        .containsEntry("code", "request-validation-failed");
  }

  @Test
  void shouldHandleFallbackException() {
    ResponseEntity<ProblemDetail> response =
        handler.handleGeneric(new RuntimeException("boom"), request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).isEqualTo("Unexpected server error");
    assertThat(response.getBody().getProperties()).containsEntry("code", "internal-server-error");
  }

  @Test
  void shouldHandleMalformedJson() {
    ResponseEntity<ProblemDetail> response =
        handler.handleMessageNotReadable(
            new HttpMessageNotReadableException("bad json"), request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).contains("Malformed JSON request");
    assertThat(response.getBody().getProperties()).containsEntry("code", "malformed-request");
  }

  @Test
  void shouldHandleAuthenticationFailure() {
    ResponseEntity<ProblemDetail> response =
        handler.handleAuthentication(new BadCredentialsException("invalid"), request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).contains("Authentication failed");
    assertThat(response.getBody().getProperties()).containsEntry("code", "authentication-failed");
  }

  @Test
  void shouldHandleConcurrencyConflict() {
    ResponseEntity<ProblemDetail> response =
        handler.handleConcurrency(new CannotAcquireLockException("locked"), request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).contains("being modified by another request");
    assertThat(response.getBody().getProperties()).containsEntry("code", "concurrency-conflict");
  }

  @Test
  void shouldHandleMissingRequestParameter() {
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("page", "int");
    ResponseEntity<ProblemDetail> response = handler.handleRequestParameter(ex, request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).contains("Missing required parameter 'page'");
    assertThat(response.getBody().getProperties())
        .containsEntry("code", "invalid-request-parameter");
  }

  @Test
  void shouldHandleTypeMismatchRequestParameter() {
    MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException("abc", Integer.class, "page", null, null);
    ResponseEntity<ProblemDetail> response = handler.handleRequestParameter(ex, request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).contains("Invalid parameter 'page'");
    assertThat(response.getBody().getProperties())
        .containsEntry("code", "invalid-request-parameter");
  }
}
