package com.bankingdemo.common.exception;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(String accountId) {
        super(ErrorCode.ACC_NOT_FOUND, "Không tìm thấy tài khoản với id: " + accountId);
    }
}
