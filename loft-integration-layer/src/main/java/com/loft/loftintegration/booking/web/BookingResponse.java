package com.loft.loftintegration.booking.web;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String propertyCode,
        String activityTypeName,
        String guestProfileId,
        String operaReservationId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        BigDecimal price,
        String currency
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getPropertyCode(),
                booking.getActivityType().getName(),
                booking.getGuestProfileId(),
                booking.getOperaReservationId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.getActivityType().getPrice(),
                booking.getActivityType().getCurrency()
        );
    }
}
