package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.notification.NotificationResponse;
import com.roomcare.backend.entity.Notification;
import com.roomcare.backend.entity.NotificationType;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;

    @Transactional
    public void createNotification(User receiver, NotificationType type, String title, String content, Long refId) {
        Notification notification = new Notification();
        notification.setUser(receiver);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRefId(refId);
        notifications.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(User user, NotificationType type, boolean unreadOnly,
                                                   int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new RuntimeException("Trang phải từ 0 và kích thước từ 1 đến 100");
        }
        var result = notifications.list(user.getId(), type, unreadOnly,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        var content = result.map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(),
                n.getContent(), n.getRefId(), n.isRead(), n.getCreatedAt())).getContent();
        return PageResponse.of(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void markRead(User user, Long id) {
        if (notifications.markRead(user.getId(), id) == 0 && !notifications.existsByIdAndUserId(id, user.getId())) {
            throw new RuntimeException("Không tìm thấy thông báo");
        }
    }

    @Transactional
    public void markAllRead(User user) {
        notifications.markAllRead(user.getId());
    }

    public long unreadCount(User user) {
        return notifications.countByUserIdAndReadFalse(user.getId());
    }
}
