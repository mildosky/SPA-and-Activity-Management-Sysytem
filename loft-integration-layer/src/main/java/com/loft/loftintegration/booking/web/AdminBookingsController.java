package com.loft.loftintegration.booking.web;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.BookingStatus;
import com.loft.loftintegration.booking.repository.BookingRepository;
import com.loft.loftintegration.booking.service.BookingService;
import com.loft.loftintegration.directory.service.GuestDirectoryService;
import com.loft.loftintegration.pos.model.Charge;
import com.loft.loftintegration.pos.model.ChargeType;
import com.loft.loftintegration.pos.repository.ChargeRepository;
import com.loft.loftintegration.pos.service.BillingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Staff-facing bookings screen: lets reception/spa staff see WHAT guests
 * have booked (from the kiosk and webshop), WHO booked it, WHEN, and the
 * current status — and act on it (confirm a tentative booking, mark an
 * appointment completed, or cancel a no-show).
 *
 * Read-only listing plus status transitions only; creating bookings stays
 * with the guest flows (kiosk/webshop) and the internal API, since the
 * availability/resource-assignment logic lives in BookingService.
 */
@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingsController {

    /** The property this install serves (adopted from property-profile.yml at startup). */
    private String PROPERTY_CODE = com.loft.loftintegration.config.LoftProperties.DEFAULT_PROPERTY_CODE;

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final GuestDirectoryService guestDirectoryService;
    private final BillingService billingService;
    private final ChargeRepository chargeRepository;
    private final com.loft.loftintegration.sync.BusinessDateHolder businessDateHolder;
    private final com.loft.loftintegration.config.LoftProperties loftProperties;

    public AdminBookingsController(BookingRepository bookingRepository,
                                   BookingService bookingService,
                                   GuestDirectoryService guestDirectoryService,
                                   BillingService billingService,
                                   ChargeRepository chargeRepository,
                                   com.loft.loftintegration.sync.BusinessDateHolder businessDateHolder,
                                   com.loft.loftintegration.config.LoftProperties loftProperties) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.guestDirectoryService = guestDirectoryService;
        this.billingService = billingService;
        this.chargeRepository = chargeRepository;
        this.businessDateHolder = businessDateHolder;
        this.loftProperties = loftProperties;
    }

    @jakarta.annotation.PostConstruct
    void resolvePropertyCode() {
        this.PROPERTY_CODE = loftProperties.currentPropertyCode();
    }

    @GetMapping
    public String listBookings(@RequestParam(required = false) LocalDate date,
                               @RequestParam(required = false) BookingStatus status,
                               Model model) {
        List<Booking> bookings;
        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();
            bookings = bookingRepository
                    .findByPropertyCodeAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                            PROPERTY_CODE, from, to);
        } else {
            bookings = bookingRepository.findByPropertyCodeOrderByStartTimeDesc(PROPERTY_CODE);
        }
        if (status != null) {
            bookings = bookings.stream().filter(b -> b.getStatus() == status).toList();
        }

        // Resolve display names: prefer the name captured at booking time
        // (kiosk/webshop), fall back to the Opera guest directory mirror
        // for profile-linked bookings, else label as Walk-in.
        Map<Long, String> guestNames = new HashMap<>();
        for (Booking b : bookings) {
            guestNames.put(b.getId(), resolveGuestName(b));
        }

        // Billing state per booking, so staff can see whether the charge made
        // it onto the guest's Opera folio — and post it if it hasn't.
        Map<Long, Charge> charges = new HashMap<>();
        for (Booking b : bookings) {
            chargeRepository.findByChargeTypeAndSourceId(ChargeType.BOOKING, b.getId())
                    .stream().findFirst()
                    .ifPresent(c -> charges.put(b.getId(), c));
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("guestNames", guestNames);
        model.addAttribute("charges", charges);
        model.addAttribute("statuses", BookingStatus.values());
        // Default the date filter to Opera's business date, not the system
        // date — kiosk bookings are created against the PMS's "today", and
        // lab/test Opera environments are deliberately backdated.
        LocalDate operaToday = businessDateHolder.today();
        model.addAttribute("selectedDate", date != null ? date : operaToday);
        model.addAttribute("today", operaToday);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Bookings - Loft Admin");
        return "admin/bookings";
    }

    private String resolveGuestName(Booking booking) {
        if (booking.getGuestName() != null && !booking.getGuestName().isBlank()) {
            return booking.getGuestName();
        }
        String profileId = booking.getGuestProfileId();
        if (profileId != null && !profileId.startsWith("WEB-")) {
            String dirName = guestDirectoryService.findGuestNameById(profileId).orElse(null);
            if (dirName != null && !dirName.isBlank()) {
                return dirName;
            }
        }
        return "Walk-in guest";
    }

    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(b);
        });
        redirectAttributes.addFlashAttribute("successMessage", "Booking confirmed.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/complete")
    public String completeBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(b);
        });
        // Auto-create the charge on completion if one doesn't exist yet
        // (webshop/staff bookings are billed here; kiosk bookings already
        // created theirs at confirm time and are left untouched).
        String message = "Booking marked as completed.";
        List<Charge> existing = chargeRepository.findByChargeTypeAndSourceId(ChargeType.BOOKING, id);
        if (existing.isEmpty()) {
            bookingRepository.findById(id).ifPresent(b -> {
                Charge charge = billingService.chargeForBooking(b);
                if (charge.getOperaReservationId() != null) {
                    redirectAttributes.addFlashAttribute("operaNote",
                            "Charge " + charge.getAmount() + " " + charge.getCurrency()
                                    + " linked to Opera reservation " + charge.getOperaReservationId()
                                    + " (status: " + charge.getStatus() + ").");
                }
            });
            message += " Charge created.";
        }
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/bookings";
    }

    /** Staff action: push this booking's charge onto the guest's Opera folio now. */
    @PostMapping("/{id}/post-to-opera")
    public String postToOpera(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        List<Charge> charges = chargeRepository.findByChargeTypeAndSourceId(ChargeType.BOOKING, id);
        if (charges.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No charge exists for this booking yet — complete it first.");
        } else {
            String outcome = billingService.postChargeToOpera(charges.get(0).getId());
            redirectAttributes.addFlashAttribute(
                    outcome.startsWith("Posted") ? "successMessage" : "errorMessage", outcome);
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled — resources freed.");
        return "redirect:/admin/bookings";
    }
}
