package com.bankingdemo.common.web;

import com.bankingdemo.common.dto.ErrorResponse;
import com.bankingdemo.common.exception.BusinessException;
import com.bankingdemo.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralized exception handling for all services (requirement doc section 10.3).
 * Never leaks stack traces to the client; always returns the standardized
 * {@link ErrorResponse} shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse body = ErrorResponse.of(
                currentTraceId(),
                errorCode.name(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ErrorResponse body = ErrorResponse.of(
                currentTraceId(),
                ErrorCode.VALIDATION_FAILED.name(),
                message.isBlank() ? ErrorCode.VALIDATION_FAILED.getDefaultMessage() : message,
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                currentTraceId(),
                ErrorCode.AUTH_FORBIDDEN_RESOURCE.name(),
                ErrorCode.AUTH_FORBIDDEN_RESOURCE.getDefaultMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.AUTH_FORBIDDEN_RESOURCE.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                currentTraceId(),
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        return traceId != null ? traceId : "unknown";
    }
}
