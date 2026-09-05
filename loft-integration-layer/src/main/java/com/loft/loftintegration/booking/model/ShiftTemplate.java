package com.loft.loftintegration.booking.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * A recurring weekly work pattern for a STAFF resource — e.g. "works
 * Mon-Fri, 9am-5pm". AvailabilityService checks this for STAFF-type
 * requirements specifically; non-staff resources (rooms, courts,
 * loungers) have no shift concept and are unaffected.
 *
 * Deliberately template-based, not per-date rows: matches TAC's own
 * "shift templates" terminology, and avoids needing a background job
 * to materialize concrete shifts into the future. A single template
 * with a Set<DayOfWeek> covers a recurring pattern in one row.
 *
 * v1 limitation: a booking must start and end on the same calendar
 * day to be checked against a shift — overnight shifts/bookings
 * aren't modeled (see AvailabilityService.isStaffOnShift()).
 */
@Entity
@Table(name = "shift_template")
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    /** Display label only, e.g. "Morning Shift" — not used in matching logic. */
    private String name;

    // EAGER on purpose: this is a small collection (a handful of days),
    // and we've already been bitten once by a lazy-loading
    // LazyInitializationException in a similar spot (see
    // AvailableSlotsService's history) — not repeating that here.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shift_template_days", joinColumns = @JoinColumn(name = "shift_template_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    protected ShiftTemplate() {
        // JPA
    }

    public ShiftTemplate(Resource resource, String name, Set<DayOfWeek> daysOfWeek,
                          LocalTime startTime, LocalTime endTime) {
        this.resource = resource;
        this.name = name;
        this.daysOfWeek = daysOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Resource getResource() { return resource; }
    public String getName() { return name; }
    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
