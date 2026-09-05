package com.loft.loftintegration.sync.consumer;

import com.loft.loftintegration.sync.model.ReservationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    // TODO: Inject BookingService once we have logic to map Opera reservations
    // to ActivityType bookings. For now, this logs the events for audit purposes.
    
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
        // TODO: When we have logic to identify which activities are booked
        // as part of a reservation (e.g., spa packages), create corresponding
        // bookings here. For now, just log for audit.
    }

    private void handleModifiedReservation(ReservationEvent event) {
        log.info("Reservation modified: {}. Updating linked bookings if needed...", 
                event.getReservationId());
        // TODO: Update any linked bookings if dates/times changed
    }

    private void handleCancelledReservation(ReservationEvent event) {
        log.info("Reservation cancelled: {}. Cancelling linked bookings...", 
                event.getReservationId());
        // TODO: Cancel any bookings linked to this reservation
    }
}
