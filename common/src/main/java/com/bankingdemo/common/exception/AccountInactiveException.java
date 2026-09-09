package com.bankingdemo.common.exception;

public class AccountInactiveException extends BusinessException {

    public AccountInactiveException(String accountId) {
        super(ErrorCode.ACC_INACTIVE, "Tài khoản " + accountId + " đã bị khoá/ngừng hoạt động");
    }
}
