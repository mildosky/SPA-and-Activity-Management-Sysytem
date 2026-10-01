package com.loft.loftintegration.webshop.service;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.BookingStatus;
import com.loft.loftintegration.booking.repository.BookingRepository;
import com.loft.loftintegration.booking.service.BookingService;
import com.loft.loftintegration.directory.service.GuestDirectoryService;
import com.loft.loftintegration.pos.service.BillingService;
import com.loft.loftintegration.webshop.model.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Guest-facing booking flow. Two pieces of logic on top of the plain
 * booking core: resolving a Customer from name/email/phone, and now
 * attempting to link that Customer to a real Opera guest profile via
 * GuestDirectoryService (see that class for how the mirror gets
 * populated, and its class doc for why matches will be rare against
 * the lab's sparse email data — a data gap, not a code gap).
 * Everything else (availability checking, resource assignment,
 * conflict detection) is the exact same BookingService/
 * AvailabilityService the internal BookingController uses — this
 * stays a thin wrapper, not a parallel implementation.
 *
 * The Customer<->Booking link is deliberately NOT a new foreign key or
 * join table: Booking.guestProfileId is reused. When a real Opera
 * match is found, guestProfileId becomes the actual Opera NAME_ID —
 * the booking is now properly attributed to the real guest, same as
 * an Opera-sourced ReservationEvent would be. When no match is found,
 * it falls back to storing "WEB-<customerId>" as before. This avoids
 * coupling the booking core's schema to either the webshop OR Opera
 * (a walk-in staff-created booking has neither) while still making
 * both kinds of link resolvable.
 *
 * Billing happens at markPaid(), NOT at createBooking() — a webshop
 * booking starts PENDING_PAYMENT and must not be charged until it's
 * actually confirmed (see BookingService's class doc for why billing
 * timing was deliberately kept out of the shared core method).
 */
@Service
public class WebshopBookingService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final CustomerService customerService;
    private final GuestDirectoryService guestDirectoryService;
    private final BillingService billingService;

    public WebshopBookingService(BookingService bookingService, BookingRepository bookingRepository,
                                  CustomerService customerService, GuestDirectoryService guestDirectoryService,
                                  BillingService billingService) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.customerService = customerService;
        this.guestDirectoryService = guestDirectoryService;
        this.billingService = billingService;
    }

    @Transactional
    public Booking createBooking(Long activityTypeId, String customerName, String customerEmail,
                                  String customerPhone, LocalDateTime startTime) {
        Customer customer = customerService.findOrCreate(customerName, customerEmail, customerPhone);

        // Try to link to a real Opera guest by email first — only fall
        // back to the synthetic webshop-only identity if no match is
        // found (or the customer has no email, though the current
        // webshop DTO always requires one).
        String guestProfileId = guestDirectoryService.findOperaNameIdByEmail(customerEmail)
                .orElse("WEB-" + customer.getId());

        // operaReservationId stays null even when the guest IS matched
        // — matching a PROFILE to a RESERVATION is a separate, harder
        // problem (which of the guest's possibly-many reservations, if
        // any, is this booking "for"?) that's out of scope here. This
        // links the guest identity, not a specific hotel stay.
        Booking booking = bookingService.createBooking(activityTypeId, guestProfileId, null, startTime);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        return bookingRepository.save(booking);
    }

    /**
     * Stub for what a real payment gateway webhook would do — marks a
     * PENDING_PAYMENT booking as CONFIRMED, and only NOW generates a
     * Charge for it (see BillingService). No actual payment processing
     * happens here; this exists so the booking flow can be exercised
     * end to end before a real gateway (Stripe/Paystack/etc.) is wired
     * in.
     */
    @Transactional
    public Booking markPaid(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("No Booking with id " + bookingId));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        billingService.chargeForBooking(booking);
        return booking;
    }
}
