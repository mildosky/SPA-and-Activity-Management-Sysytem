package com.loft.loftintegration.pos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One ad-hoc retail transaction — a guest (or an anonymous walk-in)
 * buying one or more RetailItems, not tied to any booking. Produces
 * exactly one Charge for the sale's total once finalized (see
 * PosSaleService) — individual pricing/quantity detail lives in
 * PosSaleLineItem for record-keeping, not split into separate charges.
 */
@Entity
@Table(name = "pos_sale")
public class PosSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    /** Nullable — a retail sale can be a fully anonymous walk-in with no identity captured at all. */
    @Column(nullable = true)
    private String guestProfileId;

    @Column(nullable = true)
    private String operaReservationId;

    @Column(nullable = false)
    private String currency;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PosSaleLineItem> lineItems = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected PosSale() {
        // JPA
    }

    public PosSale(String propertyCode, String guestProfileId, String operaReservationId, String currency) {
        this.propertyCode = propertyCode;
        this.guestProfileId = guestProfileId;
        this.operaReservationId = operaReservationId;
        this.currency = currency;
    }

    public void addLineItem(PosSaleLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setSale(this);
    }

    public BigDecimal total() {
        return lineItems.stream()
                .map(PosSaleLineItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public String getGuestProfileId() { return guestProfileId; }
    public String getOperaReservationId() { return operaReservationId; }
    public String getCurrency() { return currency; }
    public List<PosSaleLineItem> getLineItems() { return lineItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
