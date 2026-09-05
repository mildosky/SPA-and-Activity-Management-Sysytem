package com.loft.loftintegration.booking.web;

import java.time.LocalDateTime;

/** Request body for POST /api/bookings. */
public record CreateBookingRequest(
        Long activityTypeId,
        String guestProfileId,
        String operaReservationId,
        LocalDateTime startTime
) {
}
