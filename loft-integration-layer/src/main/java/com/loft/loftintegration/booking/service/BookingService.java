package com.loft.loftintegration.booking.service;

import com.loft.loftintegration.booking.model.*;
import com.loft.loftintegration.booking.repository.ActivityTypeRepository;
import com.loft.loftintegration.booking.repository.BookingRepository;
import com.loft.loftintegration.booking.repository.ResourceAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Creates and cancels bookings. The one method that matters here is
 * createBooking() — everything else in this module exists to support it.
 *
 * Deliberately does NOT create a Charge (see pos.service.BillingService)
 * — this method is shared by two callers with different billing
 * timing: internal staff bookings (BookingController) are already
 * CONFIRMED at creation and should be billed immediately; webshop
 * bookings (WebshopBookingService) start PENDING_PAYMENT and must NOT
 * be billed until markPaid() actually confirms them. Folding billing
 * into this shared method would either double-bill or bill before
 * payment — each caller calls BillingService itself at the right
 * point in ITS OWN lifecycle instead.
 */
@Service
public class BookingService {

    private final ActivityTypeRepository activityTypeRepository;
    private final BookingRepository bookingRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;
    private final AvailabilityService availabilityService;

    public BookingService(ActivityTypeRepository activityTypeRepository,
                           BookingRepository bookingRepository,
                           ResourceAssignmentRepository resourceAssignmentRepository,
                           AvailabilityService availabilityService) {
        this.activityTypeRepository = activityTypeRepository;
        this.bookingRepository = bookingRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.availabilityService = availabilityService;
    }

    /**
     * Books an activity for a guest (or a walk-in — guestProfileId and
     * operaReservationId are both nullable) at the given start time.
     * endTime is derived from the ActivityType's defaultDurationMinutes.
     *
     * @Transactional matters here: resource availability is checked and
     * then assigned in the same transaction, so two near-simultaneous
     * booking attempts for the last free room can't both succeed — the
     * second transaction's overlap query will see the first one's
     * now-committed assignment. (Relies on the DB's default isolation
     * level being at least READ COMMITTED, true for H2 and Postgres.)
     */
    @Transactional
    public Booking createBooking(Long activityTypeId, String guestProfileId, String operaReservationId,
                                  LocalDateTime startTime) {
        ActivityType activityType = activityTypeRepository.findById(activityTypeId)
                .orElseThrow(() -> new NoSuchElementException("No ActivityType with id " + activityTypeId));

        LocalDateTime endTime = startTime.plusMinutes(activityType.getDefaultDurationMinutes());

        Map<String, List<Resource>> assignedByRole =
                availabilityService.findAvailableResources(
                        activityType.getPropertyCode(), activityType.getRequirements(), startTime, endTime);

        Booking booking = new Booking(
                activityType.getPropertyCode(), activityType, guestProfileId, operaReservationId, startTime, endTime);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        for (Map.Entry<String, List<Resource>> entry : assignedByRole.entrySet()) {
            String role = entry.getKey();
            for (Resource resource : entry.getValue()) {
                resourceAssignmentRepository.save(new ResourceAssignment(booking, resource, role));
            }
        }

        return booking;
    }

    /**
     * Cancels a booking. Deliberately doesn't delete it or its resource
     * assignments — cancelled bookings stay in history, and the overlap
     * query already excludes CANCELLED bookings, so the resources free
     * up immediately without needing to touch ResourceAssignment rows.
     *
     * Does NOT void any existing Charge for this booking — voiding a
     * charge (especially one already POSTED_TO_OPERA) is a distinct
     * financial operation with its own implications, not guessed at
     * here as a side effect of cancellation.
     */
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("No Booking with id " + bookingId));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
}
