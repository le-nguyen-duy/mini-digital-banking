package com.bankingdemo.transactionservice.web;

import com.bankingdemo.common.dto.ErrorResponse;
import com.bankingdemo.common.exception.ErrorCode;
import com.bankingdemo.common.web.TraceIdFilter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps Resilience4j's {@link RequestNotPermitted} (thrown when the
 * {@code transactionApi} rate limiter rejects a request - requirement doc
 * section 6) into the standardized 429 ErrorResponse shape.
 */
@RestControllerAdvice
public class RateLimiterExceptionHandler {

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRequestNotPermitted(RequestNotPermitted ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                currentTraceId(),
                ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                ErrorCode.RATE_LIMIT_EXCEEDED.getDefaultMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus()).body(body);
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        return traceId != null ? traceId : "unknown";
    }
}
