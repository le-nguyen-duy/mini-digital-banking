package com.bankingdemo.common.exception;

public class ForbiddenResourceException extends BusinessException {

    public ForbiddenResourceException(String message) {
        super(ErrorCode.AUTH_FORBIDDEN_RESOURCE, message);
    }
}
