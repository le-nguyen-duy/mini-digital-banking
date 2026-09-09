package com.bankingdemo.common.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String accountId) {
        super(ErrorCode.TXN_INSUFFICIENT_BALANCE, "Số dư tài khoản " + accountId + " không đủ để thực hiện giao dịch");
    }
}
