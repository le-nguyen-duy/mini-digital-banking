package com.bankingdemo.common.dto;

import java.time.Instant;

/**
 * Standardized error response body, matching the shape defined in
 * requirement doc section 10.1:
 * { timestamp, traceId, errorCode, message, path }
 */
public record ErrorResponse(
        Instant timestamp,
        String traceId,
        String errorCode,
        String message,
        String path
) {
    public static ErrorResponse of(String traceId, String errorCode, String message, String path) {
        return new ErrorResponse(Instant.now(), traceId, errorCode, message, path);
    }
}
