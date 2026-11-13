package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldViolation> violations
) {

	public ApiError {
		violations = violations == null ? List.of() : List.copyOf(violations);
	}

	public static ApiError of(int status, String error, String message, String path, List<FieldViolation> violations) {
		return new ApiError(Instant.now(), status, error, message, path, violations);
	}

	public record FieldViolation(String field, String message) {
	}
}
