package com.loft.loftintegration.booking.model;

/**
 * Status of a recurring booking pattern.
 * 
 * ACTIVE: The recurrence is active and generating new bookings.
 * COMPLETED: All occurrences have been generated (reached endDate or totalOccurrences).
 * CANCELLED: The recurrence was cancelled before completion.
 */
public enum RecurringBookingStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
