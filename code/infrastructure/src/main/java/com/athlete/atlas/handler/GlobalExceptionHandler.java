package com.athlete.atlas.handler;

import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.exception.rest.BadRequestException;
import com.athlete.atlas.exception.rest.RestClientErrorCode;
import com.athlete.atlas.exception.rest.UnprocessableEntityException;
import com.athlete.atlas.infrastructure.api.model.Problem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into the {@code Problem} responses (application/problem+json) defined in
 * the API contract. The detail never exposes internal messages, credentials or provider responses;
 * for invalid requests it only names the offending parameters.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final URI ABOUT_BLANK = URI.create("about:blank");

  private static final String INVALID_PARAMETERS = "Invalid parameters";

  /**
   * Constraint violations on parameters of interfaces annotated with {@code @Validated} (the
   * generated APIs): validation is applied by the AOP proxy, not by Spring MVC.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Problem> handleConstraintViolation(final ConstraintViolationException ex) {
    return invalidParameters(
        ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(GlobalExceptionHandler::leafName)
            .toList());
  }

  /** Spring MVC built-in method validation (controllers without class-level @Validated). */
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<Problem> handleHandlerMethodValidation(
      final HandlerMethodValidationException ex) {
    return invalidParameters(
        ex.getParameterValidationResults().stream()
            .map(result -> result.getMethodParameter().getParameterName())
            .toList());
  }

  /** Invalid {@code @Valid @RequestBody}. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Problem> handleMethodArgumentNotValid(
      final MethodArgumentNotValidException ex) {
    return invalidParameters(
        ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).toList());
  }

  /** Values that cannot be converted to the parameter type, e.g. an unknown enum value. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Problem> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
    return invalidParameters(List.of(ex.getName()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<Problem> handleMissingParameter(
      final MissingServletRequestParameterException ex) {
    return invalidParameters(List.of(ex.getParameterName()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Problem> handleBadRequest(final BadRequestException ex) {
    log.debug("Bad request: {}", ex.getMessage());
    return problem(
        HttpStatus.BAD_REQUEST, INVALID_PARAMETERS, RestClientErrorCode.BAD_REQUEST.message());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Problem> handleNotFound(final NotFoundException ex) {
    log.debug("Resource not found: {}", ex.getMessage());
    return problem(
        HttpStatus.NOT_FOUND,
        "Resource not found",
        RestClientErrorCode.RESOURCE_NOT_FOUND.message());
  }

  @ExceptionHandler(UnprocessableEntityException.class)
  public ResponseEntity<Problem> handleUnprocessableEntity(final UnprocessableEntityException ex) {
    log.debug("Unprocessable entity: {}", ex.getMessage());
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Unprocessable entity",
        RestClientErrorCode.UNPROCESSABLE_ENTITY.message());
  }

  /**
   * Upstream failures. The upstream status is logged but never propagated: the contract always
   * answers 503 when the data cannot be obtained.
   */
  @ExceptionHandler(RemoteException.class)
  public ResponseEntity<Problem> handleRemote(final RemoteException ex) {
    log.warn("Upstream unavailable (status: {}): {}", ex.getStatusCode(), ex.getMessage());
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Data temporarily unavailable",
        RestClientErrorCode.REMOTE_API_ERROR.message());
  }

  /** Data that has not been synchronized yet: nothing to serve until the first refresh succeeds. */
  @ExceptionHandler(DataUnavailableException.class)
  public ResponseEntity<Problem> handleDataUnavailable(final DataUnavailableException ex) {
    log.debug("Data unavailable: {}", ex.getMessage());
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Data temporarily unavailable",
        "The requested data has not been synchronized yet.");
  }

  /**
   * Any other exception. Spring MVC exceptions that already carry a status (404 for an unknown
   * route, 405, 415...) keep it; everything else is a 500.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Problem> handleUnexpected(final Exception ex) {
    if (ex instanceof final ErrorResponse errorResponse) {
      final HttpStatusCode status = errorResponse.getStatusCode();
      log.debug("Request rejected by Spring MVC ({}): {}", status.value(), ex.getMessage());
      final String title =
          status instanceof final HttpStatus httpStatus
              ? httpStatus.getReasonPhrase()
              : "HTTP " + status.value();
      return problem(status, title, title + ".");
    }

    log.error("Unexpected error", ex);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        RestClientErrorCode.INTERNAL_ERROR.message());
  }

  private static ResponseEntity<Problem> invalidParameters(final Collection<String> parameters) {
    final String names =
        parameters.stream()
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.joining(", "));
    log.debug("Invalid request parameters: {}", names);
    final String detail =
        names.isEmpty()
            ? RestClientErrorCode.BAD_REQUEST.message()
            : "Invalid value for parameters: " + names + ".";
    return problem(HttpStatus.BAD_REQUEST, INVALID_PARAMETERS, detail);
  }

  /** Last node of the path, e.g. {@code limit} for {@code listAthleteSponsors.limit}. */
  private static String leafName(final Path path) {
    return StreamSupport.stream(path.spliterator(), false)
        .reduce((first, second) -> second)
        .map(Path.Node::getName)
        .orElse(null);
  }

  private static ResponseEntity<Problem> problem(
      final HttpStatusCode status, final String title, final String detail) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(new Problem(ABOUT_BLANK, title, status.value(), detail));
  }
}
