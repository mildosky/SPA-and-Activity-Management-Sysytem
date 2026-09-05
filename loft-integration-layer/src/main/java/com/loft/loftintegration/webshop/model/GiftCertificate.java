package com.loft.loftintegration.webshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A gift certificate. v1 deliberately keeps this simple — full-value,
 * single-use (REDEEMED marks the whole thing spent, no partial-balance
 * tracking). A real product would likely want partial redemption
 * (spend part of the value, keep a remaining balance) — noted here as
 * a TODO rather than guessed at, since that's a real design decision
 * (does redemption happen through this system, through Opera's folio,
 * both?) that needs answering before building it, not assuming.
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
    private BigDecimal amount;

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
    private LocalDateTime redeemedAt;

    protected GiftCertificate() {
        // JPA
    }

    public GiftCertificate(String propertyCode, String code, BigDecimal amount, String currency,
                            Customer purchaser, String recipientName, String recipientEmail,
                            LocalDateTime expiresAt) {
        this.propertyCode = propertyCode;
        this.code = code;
        this.amount = amount;
        this.currency = currency;
        this.purchaser = purchaser;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public String getCode() { return code; }
    public BigDecimal getAmount() { return amount; }
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
