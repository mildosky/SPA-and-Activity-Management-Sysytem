package com.loft.loftintegration.pos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * One line within a PosSale — a quantity of one RetailItem.
 * unitPrice is captured at the moment of sale, NOT looked up live from
 * RetailItem later — so a later price change doesn't retroactively
 * alter historical sale records.
 */
@Entity
@Table(name = "pos_sale_line_item")
public class PosSaleLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private PosSale sale;

    @ManyToOne(optional = false)
    @JoinColumn(name = "retail_item_id", nullable = false)
    private RetailItem retailItem;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    protected PosSaleLineItem() {
        // JPA
    }

    public PosSaleLineItem(RetailItem retailItem, int quantity, BigDecimal unitPrice) {
        this.retailItem = retailItem;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    void setSale(PosSale sale) { this.sale = sale; }

    public Long getId() { return id; }
    public PosSale getSale() { return sale; }
    public RetailItem getRetailItem() { return retailItem; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
