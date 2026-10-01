package com.loft.loftintegration.webshop.web;

import java.time.LocalDateTime;
import java.util.List;

public record AvailableSlotsResponse(
        Long activityTypeId,
        List<LocalDateTime> availableStartTimes
) {
}
