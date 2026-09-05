package com.loft.loftintegration.booking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A catalog entry for something guests can book — "60-Minute Deep
 * Tissue Massage", "Sunrise Yoga", "Tennis Court Hire", "18-Hole Tee
 * Time". This is the same entity whether it's a spa treatment or a
 * golf tee time — what differs between them is purely their
 * ResourceRequirements list and defaultDurationMinutes, not any code.
 */
@Entity
@Table(name = "activity_type")
public class ActivityType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityCategory category;

    @Column(nullable = false)
    private int defaultDurationMinutes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @ElementCollection
    @CollectionTable(name = "activity_type_requirement", joinColumns = @JoinColumn(name = "activity_type_id"))
    private List<ResourceRequirement> requirements = new ArrayList<>();

    protected ActivityType() {
        // JPA
    }

    public ActivityType(String propertyCode, String name, ActivityCategory category,
                         int defaultDurationMinutes, BigDecimal price, String currency) {
        this.propertyCode = propertyCode;
        this.name = name;
        this.category = category;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.price = price;
        this.currency = currency;
    }

    public void addRequirement(ResourceRequirement requirement) {
        requirements.add(requirement);
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public String getName() { return name; }
    public ActivityCategory getCategory() { return category; }
    public int getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public List<ResourceRequirement> getRequirements() { return requirements; }
}
