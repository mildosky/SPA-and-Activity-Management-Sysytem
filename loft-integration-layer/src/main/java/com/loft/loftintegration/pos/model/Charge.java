package com.loft.loftintegration.pos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single amount a guest owes — for a completed Booking, or for a
 * finalized PosSale (retail items). One entity for both cases,
 * deliberately: both are just "guest owes X for Y", matching the same
 * "one generic model instead of category-specific code" pattern used
 * throughout this product (see ActivityType, ResourceType).
 *
 * guestProfileId and operaReservationId are copied from the source
 * (Booking or PosSale/Customer) at charge-creation time — this Charge
 * doesn't hold a live reference back to its source, just enough
 * identifying info to post to Opera and for record-keeping. sourceId +
 * sourceType together identify what generated this charge, for anyone
 * tracing a charge back to its origin.
 */
@Entity
@Table(name = "pos_charge")
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    /** Opera NAME_ID if linked, "WEB-<customerId>" if not, or null for a fully anonymous walk-in retail sale. */
    @Column(nullable = true)
    private String guestProfileId;

    /** Opera RESV_NAME_ID — required for postToOpera() to succeed; null means this charge can only ever be POSTED_LOCALLY. */
    @Column(nullable = true)
    private String operaReservationId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeType chargeType;

    /** Booking.id or PosSale.id, depending on chargeType. */
    @Column(nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status = ChargeStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime postedAt;

    protected Charge() {
        // JPA
    }

    public Charge(String propertyCode, String guestProfileId, String operaReservationId,
                  BigDecimal amount, String currency, String description,
                  ChargeType chargeType, Long sourceId) {
        this.propertyCode = propertyCode;
        this.guestProfileId = guestProfileId;
        this.operaReservationId = operaReservationId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.chargeType = chargeType;
        this.sourceId = sourceId;
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public String getGuestProfileId() { return guestProfileId; }
    public String getOperaReservationId() { return operaReservationId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public ChargeType getChargeType() { return chargeType; }
    public Long getSourceId() { return sourceId; }
    public ChargeStatus getStatus() { return status; }
    public void setStatus(ChargeStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
