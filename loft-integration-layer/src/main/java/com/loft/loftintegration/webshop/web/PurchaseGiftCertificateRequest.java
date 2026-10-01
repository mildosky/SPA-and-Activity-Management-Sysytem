package com.loft.loftintegration.webshop.web;

import java.math.BigDecimal;

public record PurchaseGiftCertificateRequest(
        String propertyCode,
        BigDecimal amount,
        String currency,
        String purchaserName,
        String purchaserEmail,
        String purchaserPhone,
        String recipientName,
        String recipientEmail
) {
}
