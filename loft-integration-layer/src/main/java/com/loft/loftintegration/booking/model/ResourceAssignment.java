package com.loft.loftintegration.booking.model;

import jakarta.persistence.*;

/**
 * Links one Booking to one specific Resource fulfilling one of its
 * ActivityType's requirements (by role). A booking needing 1 ROOM + 1
 * STAFF produces two ResourceAssignment rows.
 *
 * This is the table availability checking actually queries: "is this
 * specific Resource already assigned to a non-cancelled Booking whose
 * time window overlaps the one I'm trying to book?"
 */
@Entity
@Table(name = "resource_assignment")
public class ResourceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    /** Matches the `role` on the ResourceRequirement this assignment fulfills. */
    @Column(nullable = false)
    private String role;

    protected ResourceAssignment() {
        // JPA
    }

    public ResourceAssignment(Booking booking, Resource resource, String role) {
        this.booking = booking;
        this.resource = resource;
        this.role = role;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public Resource getResource() { return resource; }
    public String getRole() { return role; }
}
