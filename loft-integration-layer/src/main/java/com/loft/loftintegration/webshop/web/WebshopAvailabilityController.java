package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.booking.service.AvailableSlotsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Lets a guest (or the kiosk UI) ask "what times are actually free" for
 * an activity on a given date, instead of guessing a startTime and
 * seeing whether POST /api/webshop/bookings succeeds or 409s. Both
 * @RequestParam names are explicit here on purpose — same reflection
 * pitfall as the earlier @PathVariable bug (see BookingController,
 * WebshopBookingController, GiftCertificateController) applies to
 * @RequestParam too, so naming these explicitly avoids repeating it.
 */
@RestController
@RequestMapping("/api/webshop/availability")
public class WebshopAvailabilityController {

    private final AvailableSlotsService availableSlotsService;

    public WebshopAvailabilityController(AvailableSlotsService availableSlotsService) {
        this.availableSlotsService = availableSlotsService;
    }

    @GetMapping
    public AvailableSlotsResponse getAvailableSlots(
            @RequestParam("activityTypeId") Long activityTypeId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            var slots = availableSlotsService.findAvailableSlots(activityTypeId, date);
            return new AvailableSlotsResponse(activityTypeId, slots);
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
