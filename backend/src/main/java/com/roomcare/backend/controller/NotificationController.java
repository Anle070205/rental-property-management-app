package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.notification.NotificationResponse;
import com.roomcare.backend.entity.NotificationType;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal User user, @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.list(user, type, unreadOnly, page, size));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> read(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.markRead(user, id);
        return ApiResponse.success("Đã đọc thông báo");
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> readAll(@AuthenticationPrincipal User user) {
        service.markAllRead(user);
        return ApiResponse.success("Đã đọc tất cả thông báo");
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.success(service.unreadCount(user));
    }
}
