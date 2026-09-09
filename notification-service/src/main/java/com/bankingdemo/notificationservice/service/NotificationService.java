package com.bankingdemo.notificationservice.service;

import com.bankingdemo.common.kafka.TransactionCompletedEvent;
import com.bankingdemo.common.kafka.TransactionFailedEvent;
import com.bankingdemo.notificationservice.domain.Notification;
import com.bankingdemo.notificationservice.domain.NotificationChannel;
import com.bankingdemo.notificationservice.domain.NotificationStatus;
import com.bankingdemo.notificationservice.dto.NotificationPageResponse;
import com.bankingdemo.notificationservice.dto.NotificationResponse;
import com.bankingdemo.notificationservice.exception.NotificationNotFoundException;
import com.bankingdemo.notificationservice.repository.NotificationRepository;
import com.bankingdemo.notificationservice.repository.NotificationSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Records a SENT/EMAIL notification for a completed transaction.
     * Idempotent: skips if a notification for this transactionId already
     * exists (requirement doc section 8.2 - protects against Kafka
     * redelivery creating duplicate rows).
     */
    @Transactional
    public void recordCompleted(TransactionCompletedEvent event) {
        if (notificationRepository.findByTransactionId(event.transactionId()).isPresent()) {
            log.info("Notification for transactionId={} already recorded, skipping (idempotent)",
                    event.transactionId());
            return;
        }

        log.info("[EMAIL] Sending notification to customer {}: transaction {} completed successfully",
                event.customerId(), event.transactionId());

        Notification notification = new Notification();
        notification.setTransactionId(event.transactionId());
        notification.setCustomerId(event.customerId());
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.SENT);
        notification.setMessage("Giao dịch " + event.transactionId() + " đã hoàn tất thành công.");

        notificationRepository.save(notification);
    }

    /**
     * Records a notification informing the customer that their transaction
     * failed. The notification itself is still considered SENT (the
     * customer was successfully informed); the failure is reflected in the
     * message content which references the transaction's errorCode.
     * Idempotent by transactionId, same as {@link #recordCompleted}.
     */
    @Transactional
    public void recordFailed(TransactionFailedEvent event) {
        if (notificationRepository.findByTransactionId(event.transactionId()).isPresent()) {
            log.info("Notification for transactionId={} already recorded, skipping (idempotent)",
                    event.transactionId());
            return;
        }

        log.info("[EMAIL] Sending notification to customer {}: transaction {} failed with error {}",
                event.customerId(), event.transactionId(), event.errorCode());

        Notification notification = new Notification();
        notification.setTransactionId(event.transactionId());
        notification.setCustomerId(event.customerId());
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.SENT);
        notification.setMessage("Giao dịch " + event.transactionId() + " thất bại (mã lỗi: " + event.errorCode() + ").");

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId.toString()));
        return NotificationResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse searchNotifications(String transactionId, String customerId, int page, int size) {
        var spec = NotificationSpecifications.withFilters(transactionId, customerId);
        var result = notificationRepository.findAll(spec, PageRequest.of(page, size));
        return NotificationPageResponse.from(result);
    }
}
