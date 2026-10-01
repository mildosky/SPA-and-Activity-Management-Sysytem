package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WebshopBookingResponse(
        Long id,
        String propertyCode,
        String activityTypeName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        String guestProfileId,
        boolean linkedToOperaGuest,
        BigDecimal price,
        String currency
) {
    public static WebshopBookingResponse from(Booking booking) {
        String guestProfileId = booking.getGuestProfileId();
        // "WEB-" prefix means WebshopBookingService fell back to the
        // synthetic webshop-only identity — no real Opera match was
        // found. Anything else is a real Opera NAME_ID.
        boolean linkedToOperaGuest = guestProfileId != null && !guestProfileId.startsWith("WEB-");

        return new WebshopBookingResponse(
                booking.getId(),
                booking.getPropertyCode(),
                booking.getActivityType().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                guestProfileId,
                linkedToOperaGuest,
                booking.getActivityType().getPrice(),
                booking.getActivityType().getCurrency()
        );
    }
}
