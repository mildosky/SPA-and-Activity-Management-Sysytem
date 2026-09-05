package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.booking.model.ActivityType;

import java.math.BigDecimal;

public record ActivityTypeResponse(
        Long id,
        String name,
        String category,
        int durationMinutes,
        BigDecimal price,
        String currency
) {
    public static ActivityTypeResponse from(ActivityType activityType) {
        return new ActivityTypeResponse(
                activityType.getId(),
                activityType.getName(),
                activityType.getCategory().name(),
                activityType.getDefaultDurationMinutes(),
                activityType.getPrice(),
                activityType.getCurrency()
        );
    }
}
