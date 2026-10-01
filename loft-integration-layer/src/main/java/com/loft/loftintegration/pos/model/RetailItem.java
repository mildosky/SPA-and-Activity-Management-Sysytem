package com.loft.loftintegration.pos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Something sellable at the POS that isn't tied to a booking — sunscreen,
 * a water bottle, a branded towel. Separate from ActivityType (which is
 * for bookable/scheduled things) since retail items have no duration or
 * resource requirements at all — they're just "name + price".
 */
@Entity
@Table(name = "retail_item")
public class RetailItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private boolean active = true;

    protected RetailItem() {
        // JPA
    }

    public RetailItem(String propertyCode, String name, BigDecimal price, String currency) {
        this.propertyCode = propertyCode;
        this.name = name;
        this.price = price;
        this.currency = currency;
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
