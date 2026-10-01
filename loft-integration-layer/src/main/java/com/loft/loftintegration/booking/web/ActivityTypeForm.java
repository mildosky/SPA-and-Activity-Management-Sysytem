package com.loft.loftintegration.booking.web;

/**
 * DTO for creating or updating an ActivityType via the admin UI.
 */
public class ActivityTypeForm {
    private Long id;
    private String name;
    private String category;
    private int defaultDurationMinutes;
    private double price;
    private String currency;

    public ActivityTypeForm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(int defaultDurationMinutes) { this.defaultDurationMinutes = defaultDurationMinutes; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
