package com.bankingdemo.common.exception;

public class TransactionNotFoundException extends BusinessException {

    public TransactionNotFoundException(String transactionId) {
        super(ErrorCode.TXN_NOT_FOUND, "Không tìm thấy giao dịch với id: " + transactionId);
    }
}
