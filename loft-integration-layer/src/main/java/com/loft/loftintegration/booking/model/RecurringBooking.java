package com.loft.loftintegration.booking.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A recurring booking pattern (e.g., a weekly fitness class every Monday at 10am).
 * 
 * This enables guests to book a repeating series of activities without having to
 * manually book each occurrence. The RecurringBooking defines the pattern, and
 * individual Booking instances are created for each occurrence based on this pattern.
 * 
 * The pattern includes:
 * - Which days of the week the recurrence happens (e.g., every Monday and Wednesday)
 * - What time it starts
 * - How long each occurrence lasts
 * - When the recurrence starts and ends (or how many occurrences)
 * - Which activity type and guest profile
 */
@Entity
@Table(name = "recurring_booking")
public class RecurringBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "activity_type_id", nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private String guestProfileId;

    @Column(nullable = true)
    private String operaReservationId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recurring_booking_days", joinColumns = @JoinColumn(name = "recurring_booking_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private java.time.LocalDate startDate;

    @Column(nullable = true)
    private java.time.LocalDate endDate;

    @Column(nullable = true)
    private Integer totalOccurrences;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringBookingStatus status = RecurringBookingStatus.ACTIVE;

    @Column(nullable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    protected RecurringBooking() {
        // JPA
    }

    public RecurringBooking(String propertyCode, ActivityType activityType, String guestProfileId,
                            String operaReservationId, Set<DayOfWeek> daysOfWeek, LocalTime startTime,
                            int durationMinutes, java.time.LocalDate startDate, java.time.LocalDate endDate,
                            Integer totalOccurrences) {
        this.propertyCode = propertyCode;
        this.activityType = activityType;
        this.guestProfileId = guestProfileId;
        this.operaReservationId = operaReservationId;
        this.daysOfWeek = daysOfWeek;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOccurrences = totalOccurrences;
    }

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public ActivityType getActivityType() { return activityType; }
    public String getGuestProfileId() { return guestProfileId; }
    public String getOperaReservationId() { return operaReservationId; }
    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public java.time.LocalDate getStartDate() { return startDate; }
    public java.time.LocalDate getEndDate() { return endDate; }
    public Integer getTotalOccurrences() { return totalOccurrences; }
    public RecurringBookingStatus getStatus() { return status; }
    public void setStatus(RecurringBookingStatus status) { this.status = status; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
}
