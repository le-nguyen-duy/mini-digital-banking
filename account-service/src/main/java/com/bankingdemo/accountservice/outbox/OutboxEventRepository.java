package com.bankingdemo.accountservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Grabs the oldest PENDING rows for relay, using FOR UPDATE SKIP LOCKED
     * so that a second account-service instance (if the service were scaled
     * out) safely skips rows already claimed by another instance instead of
     * double-publishing them. Native query because Spring Data's JPQL
     * {@code @Lock} does not expose SKIP LOCKED.
     */
    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' " +
            "ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findBatchForRelay(@Param("limit") int limit);
}
