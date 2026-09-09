package com.bankingdemo.notificationservice.exception;

import com.bankingdemo.common.exception.BusinessException;
import com.bankingdemo.common.exception.ErrorCode;

public class NotificationNotFoundException extends BusinessException {

    public NotificationNotFoundException(String notificationId) {
        super(ErrorCode.NOTIFICATION_NOT_FOUND, "Không tìm thấy thông báo với id: " + notificationId);
    }
}
