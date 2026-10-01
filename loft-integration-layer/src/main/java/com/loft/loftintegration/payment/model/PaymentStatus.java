package com.loft.loftintegration.payment.model;

/**
 * Status of a payment transaction.
 */
public enum PaymentStatus {
    PENDING,      // Transaction initiated but not yet confirmed
    COMPLETED,    // Payment successfully processed
    FAILED,       // Payment failed
    REFUNDED      // Payment was refunded
}
