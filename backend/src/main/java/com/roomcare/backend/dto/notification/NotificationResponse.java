package com.roomcare.backend.dto.notification;

import com.roomcare.backend.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, NotificationType type, String title, String content,
                                   Long refId, boolean isRead, LocalDateTime createdAt) {}
