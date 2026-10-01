package com.loft.loftintegration.reporting.web;

import java.util.List;

public record BookingsSummaryResponse(
        String propertyCode,
        long confirmed,
        long pendingPayment,
        long tentative,
        long cancelled,
        long completed,
        List<ActivityBreakdown> byActivityType
) {
    public record ActivityBreakdown(String activityTypeName, long count) {
    }
}
