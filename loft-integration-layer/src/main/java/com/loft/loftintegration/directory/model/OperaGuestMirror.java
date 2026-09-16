package com.loft.loftintegration.directory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A local, queryable mirror of one Opera guest profile (NAME row).
 * Populated by SyncEngine each time a GuestProfileEvent comes through
 * — see GuestDirectoryService.upsert(). This exists so the webshop
 * module can match a customer to a real Opera guest by email WITHOUT
 * querying Opera's Oracle database live on every booking request —
 * that would be slow and needlessly hammers the source system for
 * something that only needs to be reasonably fresh, not real-time.
 *
 * operaNameId is unique alone for v1 (single-property TGL). TODO: if
 * this ever needs to support multiple Opera properties with their own
 * NAME_ID sequences, this needs a composite unique key on
 * (propertyCode, operaNameId) instead — NAME_IDs are not guaranteed
 * globally unique across separate Opera installs.
 */
@Entity
@Table(name = "opera_guest_mirror", uniqueConstraints = @UniqueConstraint(columnNames = "operaNameId"))
public class OperaGuestMirror {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String operaNameId;

    @Column(nullable = false)
    private String propertyCode;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    @Column(nullable = false)
    private LocalDateTime lastSyncedAt;

    protected OperaGuestMirror() {
        // JPA
    }

    public OperaGuestMirror(String operaNameId, String propertyCode, String firstName,
                             String lastName, String email, String phone) {
        this.operaNameId = operaNameId;
        this.propertyCode = propertyCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void updateFrom(String propertyCode, String firstName, String lastName, String email, String phone) {
        this.propertyCode = propertyCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOperaNameId() { return operaNameId; }
    public String getPropertyCode() { return propertyCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
}
