package com.bankingdemo.notificationservice.repository;

import com.bankingdemo.notificationservice.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
        JpaSpecificationExecutor<Notification> {

    /**
     * Used by the idempotent Kafka consumer to check whether a notification
     * was already recorded for a given transactionId before inserting a new
     * one (requirement doc section 8.2).
     */
    Optional<Notification> findByTransactionId(String transactionId);
}
