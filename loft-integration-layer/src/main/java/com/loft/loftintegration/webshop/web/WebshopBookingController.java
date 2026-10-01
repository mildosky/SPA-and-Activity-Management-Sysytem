package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.service.AvailabilityService;
import com.loft.loftintegration.webshop.service.WebshopBookingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guest-facing self-booking — used for both regular activity bookings
 * AND lounger reservations. There's no separate "lounger" endpoint or
 * code path: a lounger reservation is just a booking whose ActivityType
 * happens to require a LOUNGER resource instead of a ROOM or COURT.
 * See booking.model.ResourceType.LOUNGER and the seeded "Poolside
 * Lounger Reservation" ActivityType in BookingSeedData.
 */
@RestController
@RequestMapping("/api/webshop/bookings")
public class WebshopBookingController {

    private final WebshopBookingService webshopBookingService;

    public WebshopBookingController(WebshopBookingService webshopBookingService) {
        this.webshopBookingService = webshopBookingService;
    }

    @PostMapping
    public WebshopBookingResponse create(@RequestBody CreateWebshopBookingRequest request) {
        try {
            Booking booking = webshopBookingService.createBooking(
                    request.activityTypeId(),
                    request.customerName(),
                    request.customerEmail(),
                    request.customerPhone(),
                    request.startTime());
            return WebshopBookingResponse.from(booking);
        } catch (AvailabilityService.InsufficientAvailabilityException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Stub for what a real payment gateway webhook would call — see
     * WebshopBookingService.markPaid(). Exposed as a plain endpoint for
     * now so the flow can be tested manually; a real integration would
     * call this (or the equivalent service method directly) from a
     * webhook handler, not a guest-facing button.
     */
    @PostMapping("/{id}/mark-paid")
    public WebshopBookingResponse markPaid(@PathVariable("id") Long id) {
        try {
            return WebshopBookingResponse.from(webshopBookingService.markPaid(id));
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
