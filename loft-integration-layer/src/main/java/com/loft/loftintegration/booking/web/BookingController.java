package com.loft.loftintegration.booking.web;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.service.AvailabilityService;
import com.loft.loftintegration.booking.service.BookingService;
import com.loft.loftintegration.pos.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Minimal REST surface for the booking core — enough to demonstrate the
 * full create/cancel flow end to end. NOT a designed public API — a
 * future webshop/kiosk module will likely want a richer surface
 * (availability search before committing, guest-facing responses that
 * don't leak internal IDs, etc.). This exists so the booking core can
 * be exercised and tested now, not as the final shape of the API.
 *
 * Bills immediately after a successful create — an internal/staff
 * booking is created directly as CONFIRMED (see BookingService), so
 * unlike the webshop (which bills at markPaid()), there's no separate
 * "confirmed" moment to wait for here.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BillingService billingService;

    public BookingController(BookingService bookingService, BillingService billingService) {
        this.bookingService = bookingService;
        this.billingService = billingService;
    }

    @PostMapping
    public BookingResponse create(@RequestBody CreateBookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(
                    request.activityTypeId(),
                    request.guestProfileId(),
                    request.operaReservationId(),
                    request.startTime());
            billingService.chargeForBooking(booking);
            return BookingResponse.from(booking);
        } catch (AvailabilityService.InsufficientAvailabilityException e) {
            // 409 Conflict: the request was well-formed, but the resources
            // it asked for aren't free — distinct from a 400 (bad request)
            // or 404 (activity type doesn't exist, handled below).
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public void cancel(@PathVariable("id") Long id) {
        try {
            bookingService.cancelBooking(id);
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
