package com.loft.loftintegration.webshop.web;

import java.time.LocalDateTime;

public record CreateWebshopBookingRequest(
        Long activityTypeId,
        String customerName,
        String customerEmail,
        String customerPhone,
        LocalDateTime startTime
) {
}
