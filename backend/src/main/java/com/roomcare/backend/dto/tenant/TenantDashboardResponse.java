package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TenantDashboardResponse(String fullName, String roomCode, BigDecimal amountDue,
                                      LocalDate dueDate, long unreadCount) {}
