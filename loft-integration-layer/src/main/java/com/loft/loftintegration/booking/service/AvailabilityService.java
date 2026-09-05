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
     * v1 limitation: only checks bookings that start and end on the
     * same calendar day. A booking spanning midnight is treated as
     * "not on shift" (conservatively unavailable) rather than guessed
     * at — overnight shifts/bookings aren't modeled yet.
     */
    private boolean isStaffOnShift(Resource staffResource, LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }

        List<ShiftTemplate> templates = shiftTemplateRepository.findByResourceId(staffResource.getId());
        if (templates.isEmpty()) {
            // No shift templates configured for this staff member at
            // all — conservatively unavailable rather than assuming
            // "always available" for someone with no schedule on file.
            return false;
        }

        DayOfWeek requestedDay = start.getDayOfWeek();
        LocalTime requestedStart = start.toLocalTime();
        LocalTime requestedEnd = end.toLocalTime();

        return templates.stream().anyMatch(template ->
                template.getDaysOfWeek().contains(requestedDay)
                        && !requestedStart.isBefore(template.getStartTime())
                        && !requestedEnd.isAfter(template.getEndTime()));
    }

    /** Thrown when a booking can't be satisfied — names which requirement failed and why. */
    public static class InsufficientAvailabilityException extends RuntimeException {
        public InsufficientAvailabilityException(String message) {
            super(message);
        }
    }
}
