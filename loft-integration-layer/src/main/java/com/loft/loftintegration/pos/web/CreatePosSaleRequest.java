package com.loft.loftintegration.pos.web;

import java.util.Map;

public record CreatePosSaleRequest(
        String propertyCode,
        String guestProfileId,
        String operaReservationId,
        String currency,
        Map<Long, Integer> itemQuantities
) {
}
