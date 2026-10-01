package com.loft.loftintegration.booking.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * One requirement an ActivityType has — e.g. "1 ROOM" and "1 STAFF" for
 * a massage, or "1 COURT" for tennis, or "1 TEE_TIME_SLOT" for golf.
 * An ActivityType can have several of these (a couples massage might
 * need 1 ROOM + 2 STAFF).
 *
 * `role` is a human-readable label distinguishing requirements of the
 * same ResourceType on one ActivityType (rare, but e.g. a treatment
 * needing both a "lead therapist" and an "assistant" — both STAFF, but
 * different roles). For the common case it can just restate the type,
 * e.g. role="ROOM" for a ROOM requirement.
 */
@Embeddable
public class ResourceRequirement {

    private String role;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    private int quantity;

    protected ResourceRequirement() {
        // JPA
    }

    public ResourceRequirement(String role, ResourceType resourceType, int quantity) {
        this.role = role;
        this.resourceType = resourceType;
        this.quantity = quantity;
    }

    public String getRole() { return role; }
    public ResourceType getResourceType() { return resourceType; }
    public int getQuantity() { return quantity; }
}
