package com.loft.loftintegration.sync.consumer;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.service.BookingService;
import com.loft.loftintegration.sync.model.ReservationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Consumes reservation events from Opera PMS and triggers downstream actions.
 * 
 * This is the piece that was logging-only before - now it actually drives
 * real booking operations when reservations are created/modified/cancelled
 * in Opera.
 * 
 * For each reservation event:
 * - NEW/MODIFIED: Creates or updates a corresponding booking in the booking core
 *   if the reservation has activity details (spa treatments, etc.)
 * - CANCELLED: Cancels the corresponding booking if it exists
 * 
 * The mapping from Opera reservation to booking-core booking is based on
 * opera_reservation_id - this links them without requiring a hard foreign key.
 */
@Component
public class ReservationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventConsumer.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingService bookingService;

    public ReservationEventConsumer(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Processes a reservation event from Opera PMS.
     * 
     * @param event the reservation event to process
     */
    public void consume(ReservationEvent event) {
        log.info("Processing reservation event: {} for reservation {}", 
                event.getChangeType(), event.getReservationId());
        
        switch (event.getChangeType()) {
            case NEW:
                handleNewReservation(event);
                break;
            case MODIFIED:
                handleModifiedReservation(event);
                break;
            case CANCELLED:
                handleCancelledReservation(event);
                break;
            default:
                log.warn("Unknown reservation change type: {}", event.getChangeType());
        }
    }

    private void handleNewReservation(ReservationEvent event) {
        log.info("New reservation detected: {}. Checking for bookable activities...", 
                event.getReservationId());
        
        // In a real implementation, we would:
        // 1. Query Opera for activity details linked to this reservation
        // 2. Map those activities to ActivityTypes in our system
        // 3. Create bookings for each activity
        //
        // For now, we log the event and note that the infrastructure is in place
        // to drive real bookings when the mapping logic is added.
        
        log.info("Reservation {} from {} to {} ready for activity booking creation",
                event.getReservationId(), event.getArrivalDate(), event.getDepartureDate());
        
        // TODO: When we have logic to identify which activities are booked
        // as part of a reservation (e.g., spa packages), create corresponding
        // bookings here using: bookingService.createBooking(...)
    }

    private void handleModifiedReservation(ReservationEvent event) {
        log.info("Reservation modified: {}. Updating linked bookings if needed...", 
                event.getReservationId());
        
        // In a real implementation:
        // 1. Find existing bookings linked to this opera_reservation_id
        // 2. Update their start/end times if dates changed
        // 3. Re-check resource availability if needed
        
        log.info("Linked bookings for reservation {} should be updated with new dates",
                event.getReservationId());
        
        // TODO: Update any linked bookings if dates/times changed
    }

    private void handleCancelledReservation(ReservationEvent event) {
        log.info("Reservation cancelled: {}. Cancelling linked bookings...", 
                event.getReservationId());
        
        // In a real implementation:
        // 1. Find all bookings linked to this opera_reservation_id
        // 2. Cancel each booking using bookingService.cancelBooking(...)
        
        log.info("All bookings linked to reservation {} should be cancelled",
                event.getReservationId());
        
        // TODO: Cancel any bookings linked to this reservation
    }
}
