package com.bankingdemo.common.exception;

public class InvalidAmountException extends BusinessException {

    public InvalidAmountException(String message) {
        super(ErrorCode.TXN_INVALID_AMOUNT, message);
    }
}
