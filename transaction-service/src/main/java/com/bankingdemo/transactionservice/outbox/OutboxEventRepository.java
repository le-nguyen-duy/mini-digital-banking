package com.bankingdemo.transactionservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * FOR UPDATE SKIP LOCKED so a second transaction-service instance (if
     * scaled out) safely skips rows already claimed by another instance
     * instead of double-publishing them.
     */
    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' " +
            "ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findBatchForRelay(@Param("limit") int limit);
}
