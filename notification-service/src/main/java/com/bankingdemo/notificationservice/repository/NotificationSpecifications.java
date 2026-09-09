package com.bankingdemo.notificationservice.repository;

import com.bankingdemo.notificationservice.domain.Notification;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds a composable {@link Specification} for the optional transactionId /
 * customerId filters exposed by GET /api/v1/notifications. Any combination
 * (both, either, or neither) is supported.
 */
public final class NotificationSpecifications {

    private NotificationSpecifications() {
    }

    public static Specification<Notification> withFilters(String transactionId, String customerId) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            if (transactionId != null && !transactionId.isBlank()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("transactionId"), transactionId));
            }
            if (customerId != null && !customerId.isBlank()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("customerId"), customerId));
            }
            return predicate;
        };
    }
}
