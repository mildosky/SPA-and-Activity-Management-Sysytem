package com.loft.loftintegration.payment.model;

/**
 * Supported payment gateway providers.
 */
public enum PaymentGatewayProvider {
    STRIPE,
    PAYSTACK,
    MANUAL  // For cash/check payments recorded manually
}
