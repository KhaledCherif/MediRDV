package com.medirdv.patientservice.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new HashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(errors);
	}

	@ExceptionHandler(PatientNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(PatientNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler({ DuplicateNumeroAssureException.class, DataIntegrityViolationException.class })
	public ResponseEntity<Map<String, String>> handleConflict(Exception exception) {
		String message = exception instanceof DuplicateNumeroAssureException
				? exception.getMessage()
				: "A patient with this numeroAssure already exists";
		return error(HttpStatus.CONFLICT, message);
	}

	private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
		Map<String, String> body = new HashMap<>();
		body.put("status", String.valueOf(status.value()));
		body.put("error", status.getReasonPhrase());
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
