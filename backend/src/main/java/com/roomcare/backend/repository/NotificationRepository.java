package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Notification;
import com.roomcare.backend.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
            select n from Notification n where n.user.id = :userId
                and (:type is null or n.type = :type)
                and (:unreadOnly = false or n.read = false)
            """)
    Page<Notification> list(@Param("userId") Long userId, @Param("type") NotificationType type,
                            @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.id = :id")
    int markRead(@Param("userId") Long userId, @Param("id") Long id);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId);
}
