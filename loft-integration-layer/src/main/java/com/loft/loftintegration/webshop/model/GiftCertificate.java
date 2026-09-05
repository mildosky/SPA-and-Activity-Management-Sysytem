package com.loft.loftintegration.webshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A gift certificate with support for partial-balance redemption.
 * 
 * Unlike v1's simple single-use model, this tracks a remaining balance
 * that can be spent across multiple redemptions until fully depleted.
 * Each redemption creates a GiftCertificateRedemption record for audit.
 */
@Entity
@Table(name = "gift_certificate", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class GiftCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    /** Redeemable code the recipient actually uses — separate from the internal DB id. */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    /** Current remaining balance - decreases with each partial redemption */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @Column(nullable = false)
    private String currency;

    @ManyToOne(optional = false)
    @JoinColumn(name = "purchaser_customer_id", nullable = false)
    private Customer purchaser;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GiftCertificateStatus status = GiftCertificateStatus.PENDING_PAYMENT;

    @Column(nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime expiresAt;

    @Column(nullable = true)
    private LocalDateTime redeemedAt;  // When fully redeemed (balance = 0)

    protected GiftCertificate() {
        // JPA
    }

    public GiftCertificate(String propertyCode, String code, BigDecimal amount, String currency,
                            Customer purchaser, String recipientName, String recipientEmail,
                            LocalDateTime expiresAt) {
        this.propertyCode = propertyCode;
        this.code = code;
        this.originalAmount = amount;
        this.remainingBalance = amount;
        this.currency = currency;
        this.purchaser = purchaser;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.expiresAt = expiresAt;
    }

    /**
     * Redeems a portion or all of the certificate's remaining balance.
     * 
     * @param amountToRedeem the amount to redeem
     * @throws IllegalStateException if certificate is not ACTIVE, expired, or insufficient balance
     */
    public void redeem(BigDecimal amountToRedeem) {
        if (this.status != GiftCertificateStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Certificate " + code + " is " + status + ", not ACTIVE — cannot redeem.");
        }
        if (this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now())) {
            this.setStatus(GiftCertificateStatus.EXPIRED);
            throw new IllegalStateException("Certificate " + code + " expired on " + this.expiresAt);
        }
        if (amountToRedeem.compareTo(this.remainingBalance) > 0) {
            throw new IllegalStateException(
                    "Redemption amount " + amountToRedeem + " exceeds remaining balance " + this.remainingBalance);
        }

        this.remainingBalance = this.remainingBalance.subtract(amountToRedeem);
        
        // If fully redeemed, mark as REDEEMED
        if (this.remainingBalance.compareTo(BigDecimal.ZERO) == 0) {
            this.status = GiftCertificateStatus.REDEEMED;
            this.redeemedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public String getCode() { return code; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public String getCurrency() { return currency; }
    public Customer getPurchaser() { return purchaser; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientEmail() { return recipientEmail; }
    public GiftCertificateStatus getStatus() { return status; }
    public void setStatus(GiftCertificateStatus status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
}
