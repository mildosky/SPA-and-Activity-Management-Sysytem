package com.loft.loftintegration.sync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Normalized reservation event, PMS-agnostic.
 *
 * Every connector maps its native reservation payload into this shape.
 * Downstream modules (booking, POS, staff scheduling) only ever see this
 * class — never Opera's raw XML/DB rows — so the product isn't rebuilt
 * every time a connector for a different PMS is added.
 */
public class ReservationEvent {

    public enum ChangeType { NEW, MODIFIED, CANCELLED }

    private String reservationId;
    private String propertyCode;
    private String guestProfileId;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private String roomType;
    private String roomNumber;
    private ChangeType changeType;
    private LocalDateTime eventTimestamp;

    public ReservationEvent() {
    }

    public ReservationEvent(String reservationId, String propertyCode, String guestProfileId,
                             LocalDate arrivalDate, LocalDate departureDate, String roomType,
                             String roomNumber, ChangeType changeType, LocalDateTime eventTimestamp) {
        this.reservationId = reservationId;
        this.propertyCode = propertyCode;
        this.guestProfileId = guestProfileId;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.roomType = roomType;
        this.roomNumber = roomNumber;
        this.changeType = changeType;
        this.eventTimestamp = eventTimestamp;
    }

    // Getters and setters

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getPropertyCode() { return propertyCode; }
    public void setPropertyCode(String propertyCode) { this.propertyCode = propertyCode; }

    public String getGuestProfileId() { return guestProfileId; }
    public void setGuestProfileId(String guestProfileId) { this.guestProfileId = guestProfileId; }

    public LocalDate getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDate arrivalDate) { this.arrivalDate = arrivalDate; }

    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
