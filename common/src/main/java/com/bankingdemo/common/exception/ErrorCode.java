package com.bankingdemo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Standardized error codes shared across all services.
 * See requirement doc section 10.2 for the canonical table.
 */
public enum ErrorCode {

    ACC_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
    ACC_INACTIVE(HttpStatus.CONFLICT, "Tài khoản đã bị khoá/ngừng hoạt động"),
    TXN_INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "Số dư tài khoản không đủ để thực hiện giao dịch"),
    TXN_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "Số tiền không hợp lệ"),
    TXN_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch"),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc hết hạn"),
    AUTH_FORBIDDEN_RESOURCE(HttpStatus.FORBIDDEN, "Không có quyền thao tác trên tài nguyên này"),
    ACCOUNT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "account-service tạm thời không khả dụng"),
    TXN_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "Giao dịch trùng lặp"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Vượt quá giới hạn số lượng request"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
