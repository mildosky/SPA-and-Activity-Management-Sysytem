package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.webshop.model.GiftCertificate;
import com.loft.loftintegration.webshop.service.GiftCertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/webshop/gift-certificates")
public class GiftCertificateController {

    private final GiftCertificateService giftCertificateService;

    public GiftCertificateController(GiftCertificateService giftCertificateService) {
        this.giftCertificateService = giftCertificateService;
    }

    @PostMapping
    public GiftCertificateResponse purchase(@RequestBody PurchaseGiftCertificateRequest request) {
        // v1: certificates never expire unless you pass a real business
        // reason to add expiry logic later. LocalDateTime.now().plusYears(2)
        // is a common hospitality-industry default (many jurisdictions
        // have minimum legal validity periods for gift certificates —
        // NOT legal advice, just a placeholder; check local requirements
        // before this goes to a real customer).
        LocalDateTime expiresAt = LocalDateTime.now().plusYears(2);

        GiftCertificate certificate = giftCertificateService.issue(
                request.propertyCode(), request.amount(), request.currency(),
                request.purchaserName(), request.purchaserEmail(), request.purchaserPhone(),
                request.recipientName(), request.recipientEmail(), expiresAt);

        return GiftCertificateResponse.from(certificate);
    }

    /** Stub for a real payment webhook — see GiftCertificateService.markPaid(). */
    @PostMapping("/{code}/mark-paid")
    public GiftCertificateResponse markPaid(@PathVariable("code") String code) {
        try {
            return GiftCertificateResponse.from(giftCertificateService.markPaid(code));
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{code}/redeem")
    public GiftCertificateResponse redeem(@PathVariable("code") String code) {
        try {
            return GiftCertificateResponse.from(giftCertificateService.redeem(code));
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
