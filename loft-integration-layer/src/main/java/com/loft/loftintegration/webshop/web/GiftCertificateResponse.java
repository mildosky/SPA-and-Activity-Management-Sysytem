package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.webshop.model.GiftCertificate;
import com.loft.loftintegration.webshop.model.GiftCertificateStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GiftCertificateResponse(
        String code,
        BigDecimal amount,
        String currency,
        String recipientName,
        String recipientEmail,
        GiftCertificateStatus status,
        LocalDateTime expiresAt
) {
    public static GiftCertificateResponse from(GiftCertificate certificate) {
        return new GiftCertificateResponse(
                certificate.getCode(),
                certificate.getOriginalAmount(),
                certificate.getCurrency(),
                certificate.getRecipientName(),
                certificate.getRecipientEmail(),
                certificate.getStatus(),
                certificate.getExpiresAt()
        );
    }
}
