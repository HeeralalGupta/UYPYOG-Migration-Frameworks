package org.egov.finance.migration.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.egov.finance.migration.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {

		log.error("Exception occurred at {}", request.getRequestURI(), ex);

		ErrorResponse response = ErrorResponse.builder().timestamp(java.time.LocalDateTime.now())
				.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error("Internal Server Error")
				.message(ex.getMessage() != null ? ex.getMessage() : "Something went wrong")
				.path(request.getRequestURI()).build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}