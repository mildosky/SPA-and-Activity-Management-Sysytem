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
            propertyCode = com.loft.loftintegration.config.LoftProperties.DEFAULT_PROPERTY_CODE;
        }
    }
    
    public CreateBookingRequest() {
        this(com.loft.loftintegration.config.LoftProperties.DEFAULT_PROPERTY_CODE, null, null, null, null, null, null, null, null, null);
    }
}
