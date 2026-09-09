package com.bankingdemo.notificationservice.controller;

import com.bankingdemo.notificationservice.dto.NotificationPageResponse;
import com.bankingdemo.notificationservice.dto.NotificationResponse;
import com.bankingdemo.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<NotificationPageResponse> searchNotifications(
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.searchNotifications(transactionId, customerId, page, size));
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.getNotification(notificationId));
    }
}
