package com.loft.loftintegration.webshop.service;

import com.loft.loftintegration.webshop.model.Customer;
import com.loft.loftintegration.webshop.model.GiftCertificate;
import com.loft.loftintegration.webshop.model.GiftCertificateStatus;
import com.loft.loftintegration.webshop.repository.GiftCertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class GiftCertificateService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I — avoids misread codes
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GiftCertificateRepository giftCertificateRepository;
    private final CustomerService customerService;

    public GiftCertificateService(GiftCertificateRepository giftCertificateRepository,
                                   CustomerService customerService) {
        this.giftCertificateRepository = giftCertificateRepository;
        this.customerService = customerService;
    }

    /**
     * Issues a new certificate in PENDING_PAYMENT status — not usable
     * until markPaid() is called (stubbing a real payment gateway, see
     * WebshopBookingService's similar stub).
     */
    @Transactional
    public GiftCertificate issue(String propertyCode, BigDecimal amount, String currency,
                                  String purchaserName, String purchaserEmail, String purchaserPhone,
                                  String recipientName, String recipientEmail, LocalDateTime expiresAt) {
        Customer purchaser = customerService.findOrCreate(purchaserName, purchaserEmail, purchaserPhone);
        String code = generateUniqueCode();

        GiftCertificate certificate = new GiftCertificate(
                propertyCode, code, amount, currency, purchaser, recipientName, recipientEmail, expiresAt);
        return giftCertificateRepository.save(certificate);
    }

    /** Stub for a real payment webhook — see WebshopBookingService.markPaid() for the same pattern. */
    @Transactional
    public GiftCertificate markPaid(String code) {
        GiftCertificate certificate = findByCode(code);
        certificate.setStatus(GiftCertificateStatus.ACTIVE);
        return giftCertificateRepository.save(certificate);
    }

    /**
     * Redeems a certificate for a specific amount (partial-balance support).
     * Fails if the certificate isn't ACTIVE (not paid yet, already fully redeemed,
     * expired, or cancelled) or has passed its expiry date, or if the redemption
     * amount exceeds the remaining balance.
     */
    @Transactional
    public GiftCertificate redeem(String code, BigDecimal amountToRedeem) {
        GiftCertificate certificate = findByCode(code);
        certificate.redeem(amountToRedeem);  // Uses the new partial-balance method
        return giftCertificateRepository.save(certificate);
    }

    /**
     * Legacy full-redemption method for backwards compatibility - redeems the entire remaining balance.
     */
    @Transactional
    public GiftCertificate redeem(String code) {
        GiftCertificate certificate = findByCode(code);
        return redeem(code, certificate.getRemainingBalance());
    }

    private GiftCertificate findByCode(String code) {
        return giftCertificateRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("No gift certificate with code " + code));
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (giftCertificateRepository.findByCode(code).isPresent());
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
