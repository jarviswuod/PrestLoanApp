package com.prestloan.loanengine.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  private static final String PROBLEM_BASE_URI = "https://prestloan.dev/problems/";

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(
      NotFoundException ex, HttpServletRequest request) {
    log.warn("Resource not found: path={}, message={}", request.getRequestURI(), ex.getMessage());
    return build(
        HttpStatus.NOT_FOUND,
        "resource-not-found",
        "Resource not found",
        ex.getMessage(),
        List.of(ex.getMessage()),
        request,
        ex);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();
    log.warn(
        "Request validation failed: path={}, errors={}", request.getRequestURI(), errors.size());
    return build(
        HttpStatus.BAD_REQUEST,
        "request-validation-failed",
        "Request validation failed",
        "Request validation failed",
        errors,
        request,
        ex);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    List<String> errors =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .toList();
    log.warn("Constraint violation: path={}, errors={}", request.getRequestURI(), errors.size());
    return build(
        HttpStatus.BAD_REQUEST,
        "request-validation-failed",
        "Request validation failed",
        "Request validation failed",
        errors,
        request,
        ex);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    String message = "Malformed JSON request or invalid enum/value type";
    log.warn("Malformed request payload: path={}", request.getRequestURI());
    return build(
        HttpStatus.BAD_REQUEST,
        "malformed-request",
        "Malformed request",
        message,
        List.of(message),
        request,
        ex);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ProblemDetail> handleRequestParameter(
      Exception ex, HttpServletRequest request) {
    String message;
    if (ex instanceof MethodArgumentTypeMismatchException mismatchEx) {
      String name = mismatchEx.getName();
      Object provided = mismatchEx.getValue();
      String expected =
          mismatchEx.getRequiredType() == null
              ? "unknown"
              : mismatchEx.getRequiredType().getSimpleName();
      message =
          "Invalid parameter '"
              + name
              + "': value '"
              + provided
              + "' is not of expected type "
              + expected;
    } else if (ex instanceof MissingServletRequestParameterException missingEx) {
      message = "Missing required parameter '" + missingEx.getParameterName() + "'";
    } else {
      message = "Invalid request parameter";
    }

    log.warn(
        "Invalid request parameter: path={}, message={}", request.getRequestURI(), ex.getMessage());
    return build(
        HttpStatus.BAD_REQUEST,
        "invalid-request-parameter",
        "Invalid request",
        message,
        List.of(message),
        request,
        ex);
  }

  @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
  public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
    log.warn(
        "Business validation failed: path={}, message={}",
        request.getRequestURI(),
        ex.getMessage());
    return build(
        HttpStatus.BAD_REQUEST,
        "business-validation-failed",
        "Business validation failed",
        ex.getMessage(),
        List.of(ex.getMessage()),
        request,
        ex);
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<ProblemDetail> handleUnsupportedOperation(
      UnsupportedOperationException ex, HttpServletRequest request) {
    String message =
        ex.getMessage() == null ? "Requested operation is not supported" : ex.getMessage();
    log.warn("Unsupported operation: path={}, message={}", request.getRequestURI(), message);
    return build(
        HttpStatus.BAD_REQUEST,
        "unsupported-operation",
        "Unsupported operation",
        message,
        List.of(message),
        request,
        ex);
  }

  @ExceptionHandler({
    AuthenticationException.class,
    AuthenticationCredentialsNotFoundException.class
  })
  public ResponseEntity<ProblemDetail> handleAuthentication(
      Exception ex, HttpServletRequest request) {
    String message = "Authentication failed: invalid or missing credentials";
    log.warn("Authentication failure: path={}", request.getRequestURI());
    return build(
        HttpStatus.UNAUTHORIZED,
        "authentication-failed",
        "Authentication failed",
        message,
        List.of(message),
        request,
        ex);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    String message = "Access denied";
    log.warn("Access denied: path={}", request.getRequestURI());
    return build(
        HttpStatus.FORBIDDEN,
        "authorization-failed",
        "Authorization failed",
        message,
        List.of(message),
        request,
        ex);
  }

  @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
  public ResponseEntity<ProblemDetail> handleConcurrency(Exception ex, HttpServletRequest request) {
    String message = "Loan is being modified by another request. Please retry.";
    log.warn("Concurrency conflict: path={}", request.getRequestURI());
    return build(
        HttpStatus.CONFLICT,
        "concurrency-conflict",
        "Concurrency conflict",
        message,
        List.of(message),
        request,
        ex);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
    String message = "Unexpected server error";
    log.error("Unhandled exception: path={}", request.getRequestURI(), ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal-server-error",
        "Internal server error",
        message,
        List.of(message),
        request,
        ex);
  }

  private ResponseEntity<ProblemDetail> build(
      HttpStatus status,
      String code,
      String title,
      String detail,
      List<String> errors,
      HttpServletRequest request,
      Exception ex) {
    String path = request.getRequestURI();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(PROBLEM_BASE_URI + code));
    problem.setTitle(title);
    problem.setInstance(URI.create(path));
    problem.setProperty("code", code);
    problem.setProperty("timestamp", OffsetDateTime.now());
    problem.setProperty("errors", errors);
    problem.setProperty("exception", ex.getClass().getSimpleName());
    return ResponseEntity.status(status).body(problem);
  }
}
