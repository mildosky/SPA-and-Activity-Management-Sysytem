package com.loft.loftintegration.booking.web;

/**
 * DTO for creating or updating a Resource via the admin UI.
 */
public class ResourceForm {
    private Long id;
    private String resourceType;
    private String name;
    private boolean active;

    public ResourceForm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
