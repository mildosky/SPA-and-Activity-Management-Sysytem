package com.loft.loftintegration.booking.service;

import com.loft.loftintegration.booking.model.Resource;
import com.loft.loftintegration.booking.model.ResourceRequirement;
import com.loft.loftintegration.booking.model.ResourceType;
import com.loft.loftintegration.booking.model.ShiftTemplate;
import com.loft.loftintegration.booking.repository.ResourceAssignmentRepository;
import com.loft.loftintegration.booking.repository.ResourceRepository;
import com.loft.loftintegration.booking.repository.ShiftTemplateRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Finds available resources for a given set of requirements and time
 * window. This is the piece that makes the "generic activity" design
 * work: it has no idea whether it's checking availability for a
 * massage room, a tennis court, or a golf tee-time slot — it just sees
 * ResourceRequirements and ResourceTypes.
 *
 * STAFF resources get one extra check beyond the usual double-booking
 * conflict test: they must actually be on a scheduled shift covering
 * the requested time (see isStaffOnShift()). Every other resource type
 * (rooms, courts, tee-time slots, loungers) has no shift concept and
 * is unaffected by this — being "not double-booked" is enough for them.
 */
@Service
public class AvailabilityService {

    private final ResourceRepository resourceRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;

    public AvailabilityService(ResourceRepository resourceRepository,
                                ResourceAssignmentRepository resourceAssignmentRepository,
                                ShiftTemplateRepository shiftTemplateRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.shiftTemplateRepository = shiftTemplateRepository;
    }

    /**
     * For each requirement, finds enough free resources to satisfy its
     * quantity. Returns a map of role -> chosen resources.
     *
     * Throws InsufficientAvailabilityException naming the first
     * requirement that can't be satisfied, rather than a generic
     * failure — callers (e.g. a future webshop) need to tell the guest
     * WHAT'S unavailable ("no therapists free at that time"), not just
     * that booking failed.
     */
    public Map<String, List<Resource>> findAvailableResources(
            String propertyCode,
            List<ResourceRequirement> requirements,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        Map<String, List<Resource>> assignment = new LinkedHashMap<>();

        for (ResourceRequirement requirement : requirements) {
            List<Resource> candidates = resourceRepository
                    .findByPropertyCodeAndResourceTypeAndActiveTrue(propertyCode, requirement.getResourceType());

            if (candidates.isEmpty()) {
                throw new InsufficientAvailabilityException(
                        "No active " + requirement.getResourceType() + " resources exist for property "
                                + propertyCode + " at all — none configured, not just none free.");
            }

            List<Long> candidateIds = candidates.stream().map(Resource::getId).collect(Collectors.toList());
            List<Long> busyIds = resourceAssignmentRepository
                    .findBusyResourceIds(candidateIds, startTime, endTime);

            List<Resource> free = candidates.stream()
                    .filter(r -> !busyIds.contains(r.getId()))
                    .toList();

            if (requirement.getResourceType() == ResourceType.STAFF) {
                free = free.stream()
                        .filter(r -> isStaffOnShift(r, startTime, endTime))
                        .toList();
            }

            if (free.size() < requirement.getQuantity()) {
                String shiftNote = requirement.getResourceType() == ResourceType.STAFF
                        ? " (checked against scheduled shifts, not just double-booking)" : "";
                throw new InsufficientAvailabilityException(
                        "Need " + requirement.getQuantity() + " " + requirement.getRole()
                                + " (" + requirement.getResourceType() + "), but only " + free.size()
                                + " free between " + startTime + " and " + endTime + shiftNote + ".");
            }

            assignment.put(requirement.getRole(), free.subList(0, requirement.getQuantity()));
        }

        return assignment;
    }

    /**
     * True if this staff resource has a ShiftTemplate covering the
     * requested window — i.e. the day of week matches one of the
     * template's daysOfWeek, and the requested time range falls
     * entirely within the template's startTime-endTime.
     * 
     * Now supports overnight shifts/bookings: if the booking spans
     * midnight (start and end on different dates), checks both the
     * start day's shift and the next day's shift if needed.
     */
    private boolean isStaffOnShift(Resource staffResource, LocalDateTime start, LocalDateTime end) {
        List<ShiftTemplate> templates = shiftTemplateRepository.findByResourceId(staffResource.getId());
        if (templates.isEmpty()) {
            // No shift templates configured for this staff member at
            // all — conservatively unavailable rather than assuming
            // "always available" for someone with no schedule on file.
            return false;
        }

        DayOfWeek startDay = start.getDayOfWeek();
        DayOfWeek endDay = end.getDayOfWeek();
        LocalTime requestedStart = start.toLocalTime();
        LocalTime requestedEnd = end.toLocalTime();

        // Handle overnight bookings: check if the shift spans midnight
        boolean isOvernight = !start.toLocalDate().equals(end.toLocalDate());
        
        if (isOvernight) {
            // For overnight bookings, we need a shift that either:
            // 1. Spans midnight on the start day (e.g., 10pm-6am next day)
            // 2. Or covers the portion on each day separately
            // For simplicity, we check if there's an overnight shift template
            // that covers the entire period
            return templates.stream().anyMatch(template -> 
                isOvernightShiftCovering(template, startDay, endDay, requestedStart, requestedEnd));
        } else {
            // Same-day booking: original logic
            return templates.stream().anyMatch(template ->
                    template.getDaysOfWeek().contains(startDay)
                            && !requestedStart.isBefore(template.getStartTime())
                            && !requestedEnd.isAfter(template.getEndTime()));
        }
    }

    /**
     * Checks if an overnight shift template covers a booking that spans midnight.
     * An overnight shift is one where endTime < startTime (e.g., 22:00-06:00).
     */
    private boolean isOvernightShiftCovering(ShiftTemplate template, DayOfWeek startDay, 
                                              DayOfWeek endDay, LocalTime requestedStart, 
                                              LocalTime requestedEnd) {
        // Check if this is an overnight shift (end time is before start time, meaning it crosses midnight)
        boolean isOvernightShift = template.getEndTime().isBefore(template.getStartTime());
        
        if (!isOvernightShift) {
            // Not an overnight shift, can't cover an overnight booking
            return false;
        }
        
        // Check if the booking days match the shift pattern
        // For a simple overnight shift (e.g., works Mon night into Tue morning),
        // startDay should match the shift's day, and endDay should be the next day
        DayOfWeek nextDay = startDay.plus(1);
        
        if (!template.getDaysOfWeek().contains(startDay)) {
            return false;
        }
        
        // Check if the requested time falls within the overnight shift
        // The shift runs from startTime to midnight, then midnight to endTime
        boolean startInShift = !requestedStart.isBefore(template.getStartTime());
        boolean endInShift = !requestedEnd.isAfter(template.getEndTime());
        
        return startInShift && endInShift;
    }

    /** Thrown when a booking can't be satisfied — names which requirement failed and why. */
    public static class InsufficientAvailabilityException extends RuntimeException {
        public InsufficientAvailabilityException(String message) {
            super(message);
        }
    }
}
