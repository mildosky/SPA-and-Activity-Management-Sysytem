package com.loft.loftintegration.booking.service;

import com.loft.loftintegration.booking.model.ActivityType;
import com.loft.loftintegration.booking.repository.ActivityTypeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Finds bookable time slots for an ActivityType on a given date.
 *
 * Business hours come from application.yml
 * (loft-integration.business-hours.open-time/close-time), not hardcoded
 * in Java — but it's still ONE global setting for every property and
 * activity, not per-property. Real per-property (and possibly
 * per-activity) opening hours would need a genuine Property domain
 * concept on the booking side, which doesn't exist yet (Opera's
 * PropertyProfile is connector config, not a booking-core concept —
 * see README). Not guessed at here; left as an explicit gap.
 *
 * Slots are spaced by the activity's own defaultDurationMinutes,
 * back-to-back and non-overlapping, starting at open time. The last
 * slot must fully fit before close time.
 */
@Service
public class AvailableSlotsService {

    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final ActivityTypeRepository activityTypeRepository;
    private final AvailabilityService availabilityService;

    public AvailableSlotsService(ActivityTypeRepository activityTypeRepository,
                                  AvailabilityService availabilityService,
                                  @Value("${loft-integration.business-hours.open-time:09:00}") String openTimeStr,
                                  @Value("${loft-integration.business-hours.close-time:18:00}") String closeTimeStr) {
        this.activityTypeRepository = activityTypeRepository;
        this.availabilityService = availabilityService;
        this.openTime = LocalTime.parse(openTimeStr);
        this.closeTime = LocalTime.parse(closeTimeStr);
    }

    /**
     * Returns every start time on the given date where the activity
     * could actually be booked right now — i.e. where AvailabilityService
     * confirms all its ResourceRequirements can be satisfied. Read-only:
     * checking availability here doesn't reserve anything, so a slot can
     * still be taken by someone else between checking and booking (the
     * real conflict check happens again, safely, inside
     * BookingService.createBooking()'s transaction).
     *
     * @Transactional(readOnly = true) matters here, not just as a nice-to-
     * have: ActivityType.requirements is a lazy @ElementCollection, and
     * with open-in-view disabled the Hibernate session closes right after
     * findById() returns unless this method itself keeps one open —
     * without this annotation, accessing activityType.getRequirements()
     * later in the loop throws LazyInitializationException. Hit this for
     * real; not a hypothetical.
     */
    @Transactional(readOnly = true)
    public List<LocalDateTime> findAvailableSlots(Long activityTypeId, LocalDate date) {
        ActivityType activityType = activityTypeRepository.findById(activityTypeId)
                .orElseThrow(() -> new NoSuchElementException("No ActivityType with id " + activityTypeId));

        int durationMinutes = activityType.getDefaultDurationMinutes();
        List<LocalDateTime> availableSlots = new ArrayList<>();

        LocalDateTime slotStart = date.atTime(openTime);
        LocalDateTime closeDateTime = date.atTime(closeTime);

        while (!slotStart.plusMinutes(durationMinutes).isAfter(closeDateTime)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(durationMinutes);

            if (isSlotAvailable(activityType, slotStart, slotEnd)) {
                availableSlots.add(slotStart);
            }

            slotStart = slotStart.plusMinutes(durationMinutes);
        }

        return availableSlots;
    }

    /**
     * Reuses AvailabilityService.findAvailableResources() purely for its
     * checking logic — same code path real bookings use, so results
     * can't drift out of sync with what booking creation actually
     * enforces. Read-only: this call has no side effects.
     */
    private boolean isSlotAvailable(ActivityType activityType, LocalDateTime start, LocalDateTime end) {
        try {
            availabilityService.findAvailableResources(
                    activityType.getPropertyCode(), activityType.getRequirements(), start, end);
            return true;
        } catch (AvailabilityService.InsufficientAvailabilityException e) {
            return false;
        }
    }
}
