package com.cocos.challenge.infrastructure.interfaces.rest.error;

import com.cocos.challenge.domain.exception.*;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.ApiError;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.ApiError.FieldViolation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Log4j2
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldViolation(error.getField(), messageOrDefault(error.getDefaultMessage())))
				.toList();
		logRejected(request, violations);
		return response(HttpStatus.BAD_REQUEST, "VALIDATION", "The request has invalid data", request, violations);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getConstraintViolations().stream()
				.map(violation -> new FieldViolation(violation.getPropertyPath().toString(), messageOrDefault(violation.getMessage())))
				.toList();
		logRejected(request, violations);
		return response(HttpStatus.BAD_REQUEST, "VALIDATION", "The request has invalid data", request, violations);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiError> handleInvalidParameters(HandlerMethodValidationException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldViolation(parameterName(result), messageOrDefault(error.getDefaultMessage()))))
				.toList();
		logRejected(request, violations);
		return response(HttpStatus.BAD_REQUEST, "VALIDATION", "The request has invalid data", request, violations);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		log.error("{} {} - request body could not be read: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return response(HttpStatus.BAD_REQUEST, "VALIDATION", "The request body could not be read", request, List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		String field = ex.getName() == null ? "parameter" : ex.getName();
		List<FieldViolation> violations = List.of(new FieldViolation(field, "The value does not have the expected format"));
		logRejected(request, violations);
		return response(HttpStatus.BAD_REQUEST, "VALIDATION", "The request has invalid data", request, violations);
	}

	@ExceptionHandler(InvalidOrderRequestException.class)
	public ResponseEntity<ApiError> handleValidation(RuntimeException ex, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "VALIDATION", ex.getMessage(), request, List.of());
	}

	@ExceptionHandler({
			UserNotFoundException.class,
			InstrumentNotFoundException.class,
			OrderNotFoundException.class
	})
	public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(OrderNotCancellableException.class)
	public ResponseEntity<ApiError> handleConflict(OrderNotCancellableException ex, HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleMissingRoute(NoResourceFoundException ex, HttpServletRequest request) {
		log.error("{} {} - no route matches the request", request.getMethod(), request.getRequestURI());
		return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnhandled(Exception ex, HttpServletRequest request) {
		log.error("Unhandled error at {}", request.getRequestURI(), ex);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", request, List.of());
	}

	private static ResponseEntity<ApiError> response(HttpStatus status, String error, String message,
			HttpServletRequest request, List<FieldViolation> violations) {
		return ResponseEntity.status(status)
				.body(ApiError.of(status.value(), error, message, request.getRequestURI(), violations));
	}

	private static void logRejected(HttpServletRequest request, List<FieldViolation> violations) {
		log.error("{} {} - request rejected by validation: {}", request.getMethod(), request.getRequestURI(), violations);
	}

	private static String parameterName(ParameterValidationResult result) {
		RequestParam requestParam = result.getMethodParameter().getParameterAnnotation(RequestParam.class);
		if (requestParam != null && !requestParam.name().isBlank()) {
			return requestParam.name();
		}
		String name = result.getMethodParameter().getParameterName();
		return name == null ? "parameter" : name;
	}

	private static String messageOrDefault(String message) {
		return message == null || message.isBlank() ? "invalid value" : message;
	}
}
