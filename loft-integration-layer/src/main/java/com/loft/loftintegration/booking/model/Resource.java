package com.loft.loftintegration.booking.model;

import jakarta.persistence.*;

/**
 * A single bookable resource — a specific room, a specific staff
 * member, a specific tennis court, one tee-time slot on one hole, a
 * piece of equipment. Bookings don't reference a resource TYPE
 * directly; they reference actual Resource rows, so double-booking the
 * same physical room or the same therapist is something the database
 * can actually prevent (see ResourceAssignment's overlap check).
 */
@Entity
@Table(name = "booking_resource")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which property this resource belongs to — mirrors PropertyProfile.propertyCode from the Opera layer. */
    @Column(nullable = false)
    private String propertyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    /** Display name — "Treatment Room 2", "Sarah (Massage Therapist)", "Court 1", "Hole 7 - 8:00 slot". */
    @Column(nullable = false)
    private String name;

    /** Soft-disable a resource (e.g. a room under maintenance) without deleting its booking history. */
    @Column(nullable = false)
    private boolean active = true;

    protected Resource() {
        // JPA
    }

    public Resource(String propertyCode, ResourceType resourceType, String name) {
        this.propertyCode = propertyCode;
        this.resourceType = resourceType;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public ResourceType getResourceType() { return resourceType; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
