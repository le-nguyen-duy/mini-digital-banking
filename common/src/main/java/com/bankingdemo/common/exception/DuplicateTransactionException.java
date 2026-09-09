package com.bankingdemo.common.exception;

public class DuplicateTransactionException extends BusinessException {

    public DuplicateTransactionException(String idempotencyKey) {
        super(ErrorCode.TXN_DUPLICATE_REQUEST, "Giao dịch trùng lặp với Idempotency-Key: " + idempotencyKey);
    }
}
