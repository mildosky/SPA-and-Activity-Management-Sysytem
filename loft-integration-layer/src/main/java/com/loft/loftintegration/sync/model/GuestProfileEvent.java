package com.loft.loftintegration.sync.model;

import java.time.LocalDateTime;

/**
 * Normalized guest profile event, PMS-agnostic.
 *
 * Deliberately minimal for v1 — just enough to link a spa/activity booking
 * back to the right guest. Expand as later modules need more fields
 * (loyalty tier, preferences, marketing consent, etc.).
 */
public class GuestProfileEvent {

    public enum ChangeType { NEW, MODIFIED }

    private String guestProfileId;
    private String propertyCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private ChangeType changeType;
    private LocalDateTime eventTimestamp;

    public GuestProfileEvent() {
    }

    public GuestProfileEvent(String guestProfileId, String propertyCode, String firstName,
                              String lastName, String email, String phone,
                              ChangeType changeType, LocalDateTime eventTimestamp) {
        this.guestProfileId = guestProfileId;
        this.propertyCode = propertyCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.changeType = changeType;
        this.eventTimestamp = eventTimestamp;
    }

    public String getGuestProfileId() { return guestProfileId; }
    public void setGuestProfileId(String guestProfileId) { this.guestProfileId = guestProfileId; }

    public String getPropertyCode() { return propertyCode; }
    public void setPropertyCode(String propertyCode) { this.propertyCode = propertyCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
