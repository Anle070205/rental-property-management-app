package com.roomcare.backend.service;

import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.TenantDashboardResponse;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantPortalService {
    private final TenantRepository tenants;
    private final NotificationService notifications;
    private final ObjectProvider<TenantPortalProvider> provider;

    public TenantDashboardResponse dashboard(User user) {
        Tenant tenant = tenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        var amountDue = source == null ? null : source.amountDue(tenant).orElse(null);
        return new TenantDashboardResponse(user.getFullName(),
                source == null ? null : source.activeRoomCode(tenant).orElse(null),
                amountDue == null ? null : amountDue.amount(), amountDue == null ? null : amountDue.dueDate(),
                notifications.unreadCount(user));
    }

    public MyRoomResponse room(User user) {
        Tenant tenant = tenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        return source == null ? MyRoomResponse.empty() : source.room(tenant).orElseGet(MyRoomResponse::empty);
    }

    private Tenant tenantFor(User user) {
        return tenants.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản chưa được liên kết với người thuê"));
    }
}
