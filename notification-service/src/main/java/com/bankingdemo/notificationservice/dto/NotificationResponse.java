package com.bankingdemo.notificationservice.dto;

import com.bankingdemo.notificationservice.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        UUID transactionId,
        String customerId,
        String channel,
        String status,
        String message,
        Instant sentAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                UUID.fromString(notification.getTransactionId()),
                notification.getCustomerId(),
                notification.getChannel().name(),
                notification.getStatus().name(),
                notification.getMessage(),
                notification.getSentAt()
        );
    }
}
