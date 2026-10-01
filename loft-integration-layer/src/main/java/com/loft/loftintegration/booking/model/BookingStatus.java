package com.loft.loftintegration.booking.model;

/**
 * PENDING_PAYMENT is distinct from TENTATIVE: TENTATIVE means "not yet
 * confirmed by staff" (an internal booking-core concept), PENDING_PAYMENT
 * means "the resources are reserved, but the guest hasn't paid yet" (a
 * webshop concept — see webshop.service.WebshopBookingService). Both
 * hold their resource assignments and count toward availability
 * conflicts the same way CONFIRMED does; only CANCELLED frees resources.
 */
public enum BookingStatus {
    TENTATIVE,
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
