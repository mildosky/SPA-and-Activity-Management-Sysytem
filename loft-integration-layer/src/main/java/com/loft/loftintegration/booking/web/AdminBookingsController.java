package com.loft.loftintegration.booking.web;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.BookingStatus;
import com.loft.loftintegration.booking.repository.BookingRepository;
import com.loft.loftintegration.booking.service.BookingService;
import com.loft.loftintegration.directory.service.GuestDirectoryService;
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

    private static final String PROPERTY_CODE = "LOFT";

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final GuestDirectoryService guestDirectoryService;

    public AdminBookingsController(BookingRepository bookingRepository,
                                   BookingService bookingService,
                                   GuestDirectoryService guestDirectoryService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.guestDirectoryService = guestDirectoryService;
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

        model.addAttribute("bookings", bookings);
        model.addAttribute("guestNames", guestNames);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedDate", date != null ? date : LocalDate.now());
        model.addAttribute("today", LocalDate.now());
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
        redirectAttributes.addFlashAttribute("successMessage", "Booking marked as completed.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled — resources freed.");
        return "redirect:/admin/bookings";
    }
}
