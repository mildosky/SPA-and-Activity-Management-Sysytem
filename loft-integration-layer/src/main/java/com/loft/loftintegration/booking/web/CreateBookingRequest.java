package com.loft.loftintegration.booking.web;

import java.time.LocalDateTime;

/** Request body for POST /api/bookings. */
public record CreateBookingRequest(
        String propertyCode,
        Long activityTypeId,
        String guestProfileId,
        String operaReservationId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String guestName,
        String guestEmail,
        String guestPhone,
        String notes
) {
    public CreateBookingRequest {
        if (propertyCode == null) {
            propertyCode = "LOFT";
        }
    }
    
    public CreateBookingRequest() {
        this("LOFT", null, null, null, null, null, null, null, null, null);
    }
}
