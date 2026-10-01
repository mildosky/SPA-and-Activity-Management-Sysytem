package com.loft.loftintegration.booking.model;

/**
 * The kinds of resources a booking can require. Deliberately generic —
 * a treatment needs a ROOM and a STAFF member, tennis needs a COURT,
 * golf needs a TEE_TIME_SLOT — but they're all just "resources" to the
 * scheduling engine. Add new types here as new activity categories need
 * them; the availability-checking logic doesn't need to change.
 */
public enum ResourceType {
    ROOM,
    STAFF,
    COURT,
    TEE_TIME_SLOT,
    EQUIPMENT,
    LOUNGER
}
