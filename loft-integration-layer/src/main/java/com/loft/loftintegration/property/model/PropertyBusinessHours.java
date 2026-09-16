package com.loft.loftintegration.property.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Business hours configuration for a specific property and day of week.
 * Allows per-property configurable operating hours.
 */
@Entity
@Table(name = "property_business_hours", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_code", "day_of_week"}))
public class PropertyBusinessHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime openingTime;

    @Column(nullable = false)
    private LocalTime closingTime;

    @Column(nullable = false)
    private boolean isOpen = true;

    protected PropertyBusinessHours() {
        // JPA
    }

    public PropertyBusinessHours(String propertyCode, DayOfWeek dayOfWeek, 
                                  LocalTime openingTime, LocalTime closingTime, boolean isOpen) {
        this.propertyCode = propertyCode;
        this.dayOfWeek = dayOfWeek;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.isOpen = isOpen;
    }

    // Getters and setters

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    /**
     * Checks if a given time falls within business hours for this day.
     */
    public boolean isWithinBusinessHours(LocalTime time) {
        if (!isOpen) {
            return false;
        }
        return !time.isBefore(openingTime) && !time.isAfter(closingTime);
    }
}
