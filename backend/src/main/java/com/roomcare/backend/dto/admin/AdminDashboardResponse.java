package com.roomcare.backend.dto.admin;

import java.util.List;

public record AdminDashboardResponse(
        String adminName,
        long totalAccounts,
        long landlordCount,
        long tenantCount,
        long adminCount,
        long propertyCount,
        long totalRooms,
        long occupiedRooms,
        long activeAccounts,
        long lockedAccounts,
        double activePercentage,
        List<MonthlyPoint> monthlyGrowth,
        List<RecentActivity> recentActivities
) {
    public record MonthlyPoint(String label, long count) {}
    public record RecentActivity(String timeLabel, String title, String subtitle, String type, Long refId) {}
}
