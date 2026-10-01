package com.loft.loftintegration.directory.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A local, queryable mirror of one Opera in-house reservation (RESERVATION_NAME row).
 * Populated by SyncEngine each time a ReservationEvent comes through — see
 * InHouseReservationService.upsert(). This exists so the kiosk can resolve a
 * checked-in guest to their CURRENT hotel stay (RESV_NAME_ID + room) WITHOUT
 * querying Opera's Oracle database live on every booking — same rationale as
 * OperaGuestMirror for profiles.
 *
 * Only reservations that are currently in-house (arrival <= today <= departure)
 * are kept here; checkouts drop out on the next sync pass.
 */
@Entity
@Table(name = "in_house_reservation",
        uniqueConstraints = @UniqueConstraint(columnNames = "operaReservationId"))
public class InHouseReservationMirror {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opera RESV_NAME_ID — the key charges get posted against. */
    @Column(nullable = false, unique = true)
    private String operaReservationId;

    /** Opera NAME_ID of the guest this reservation belongs to. */
    @Column(nullable = false)
    private String guestProfileId;

    @Column(nullable = false)
    private String propertyCode;

    private String firstName;
    private String lastName;
    private String roomNumber;
    private LocalDate arrivalDate;
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalDateTime lastSyncedAt = LocalDateTime.now();

    protected InHouseReservationMirror() {
        // JPA
    }

    public InHouseReservationMirror(String operaReservationId, String guestProfileId, String propertyCode,
                                     String firstName, String lastName, String roomNumber,
                                     LocalDate arrivalDate, LocalDate departureDate) {
        this.operaReservationId = operaReservationId;
        this.guestProfileId = guestProfileId;
        this.propertyCode = propertyCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roomNumber = roomNumber;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void updateFrom(String guestProfileId, String firstName, String lastName, String roomNumber,
                            LocalDate arrivalDate, LocalDate departureDate) {
        this.guestProfileId = guestProfileId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roomNumber = roomNumber;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOperaReservationId() { return operaReservationId; }
    public String getGuestProfileId() { return guestProfileId; }
    public String getPropertyCode() { return propertyCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRoomNumber() { return roomNumber; }
    public LocalDate getArrivalDate() { return arrivalDate; }
    public LocalDate getDepartureDate() { return departureDate; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }

    public boolean isCurrentlyInHouse(LocalDate today) {
        return arrivalDate != null && departureDate != null
                && !today.isBefore(arrivalDate) && !today.isAfter(departureDate);
    }
}
