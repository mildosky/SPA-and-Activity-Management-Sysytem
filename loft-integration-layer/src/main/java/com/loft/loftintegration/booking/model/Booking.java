package com.loft.loftintegration.booking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One scheduled instance of an ActivityType for a guest — "Jane Doe,
 * Deep Tissue Massage, tomorrow 2pm-3pm".
 *
 * guestProfileId and operaReservationId are both nullable and both
 * plain strings, deliberately loosely coupled to the Opera integration
 * layer rather than a hard foreign key: this module needs to work
 * standalone (a walk-in spa booking with no hotel reservation at all
 * is completely normal), and needs to work against ANY future PMS
 * connector, not just Opera — see sync.model.ReservationEvent /
 * GuestProfileEvent for the matching IDs on that side. Reconciling
 * these into a real link (e.g. a lookup service that resolves a
 * guestProfileId to full guest details from the Opera layer) is
 * intentionally left for when a real UI needs to show that guest's
 * name — this module doesn't need to know anything about Opera to
 * function.
 */
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "activity_type_id", nullable = false)
    private ActivityType activityType;

    /** Opera NAME_ID, if this booking is tied to a known guest profile. Null for walk-ins with no profile on file. */
    @Column(nullable = true)
    private String guestProfileId;

    /** Opera RESV_NAME_ID, if this booking is tied to a hotel reservation. Null for day-guests / walk-ins. */
    @Column(nullable = true)
    private String operaReservationId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.TENTATIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Booking() {
        // JPA
    }

    public Booking(String propertyCode, ActivityType activityType, String guestProfileId,
                    String operaReservationId, LocalDateTime startTime, LocalDateTime endTime) {
        this.propertyCode = propertyCode;
        this.activityType = activityType;
        this.guestProfileId = guestProfileId;
        this.operaReservationId = operaReservationId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public ActivityType getActivityType() { return activityType; }
    public String getGuestProfileId() { return guestProfileId; }
    public String getOperaReservationId() { return operaReservationId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
