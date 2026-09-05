package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.booking.model.ActivityType;
import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.Resource;
import com.loft.loftintegration.booking.repository.ActivityTypeRepository;
import com.loft.loftintegration.booking.repository.ResourceRepository;
import com.loft.loftintegration.booking.service.BookingService;
import com.loft.loftintegration.booking.web.CreateBookingRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Kiosk controller for a native kiosk mode experience.
 * Provides a simplified, touch-friendly booking interface for on-property kiosks.
 */
@Controller
@RequestMapping("/kiosk")
public class KioskController {

    private final ActivityTypeRepository activityTypeRepository;
    private final ResourceRepository resourceRepository;
    private final BookingService bookingService;

    public KioskController(ActivityTypeRepository activityTypeRepository,
                           ResourceRepository resourceRepository,
                           BookingService bookingService) {
        this.activityTypeRepository = activityTypeRepository;
        this.resourceRepository = resourceRepository;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String kioskHome(Model model) {
        List<ActivityType> activities = activityTypeRepository.findByPropertyCode("LOFT");
        model.addAttribute("activities", activities);
        model.addAttribute("pageTitle", "Loft Kiosk");
        return "kiosk/home";
    }

    @GetMapping("/activity/{id}")
    public String selectActivity(@PathVariable Long id, Model model) {
        ActivityType activity = activityTypeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        
        LocalDate today = LocalDate.now();
        List<LocalDate> next7Days = java.util.stream.IntStream.range(0, 7)
            .mapToObj(i -> today.plusDays(i))
            .toList();
        
        model.addAttribute("activity", activity);
        model.addAttribute("dates", next7Days);
        model.addAttribute("dateFormat", DateTimeFormatter.ofPattern("EEE, MMM d"));
        model.addAttribute("pageTitle", "Select Date - Loft Kiosk");
        return "kiosk/select-date";
    }

    @GetMapping("/activity/{id}/book")
    public String bookActivity(@PathVariable Long id,
                               @RequestParam String date,
                               @RequestParam String time,
                               Model model) {
        ActivityType activity = activityTypeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        
        LocalDateTime startDateTime = LocalDateTime.parse(date + "T" + time);
        
        // Create a simple guest info form
        model.addAttribute("activity", activity);
        model.addAttribute("dateTime", startDateTime);
        model.addAttribute("date", date);
        model.addAttribute("time", time);
        model.addAttribute("pageTitle", "Guest Information - Loft Kiosk");
        return "kiosk/guest-info";
    }

    @PostMapping("/confirm")
    public String confirmBooking(@RequestParam Long activityId,
                                 @RequestParam String dateTime,
                                 @RequestParam String guestName,
                                 @RequestParam(required = false) String guestEmail,
                                 @RequestParam(required = false) String guestPhone,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            ActivityType activity = activityTypeRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
            
            LocalDateTime startDateTime = LocalDateTime.parse(dateTime);
            
            CreateBookingRequest request = new CreateBookingRequest();
            request.setPropertyCode("LOFT");
            request.setActivityTypeId(activityId);
            request.setStartTime(startDateTime);
            request.setEndTime(startDateTime.plusMinutes(activity.getDefaultDurationMinutes()));
            request.setGuestName(guestName);
            request.setGuestEmail(guestEmail != null ? guestEmail : "kiosk@loft.com");
            request.setGuestPhone(guestPhone);
            request.setNotes("Kiosk booking");
            
            bookingService.createBooking(request);
            
            redirectAttributes.addFlashAttribute("successMessage", "Booking confirmed!");
            redirectAttributes.addFlashAttribute("guestName", guestName);
            redirectAttributes.addFlashAttribute("activityName", activity.getName());
            redirectAttributes.addFlashAttribute("bookingTime", dateTime);
            return "redirect:/kiosk/success";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not complete booking: " + e.getMessage());
            model.addAttribute("pageTitle", "Booking Error - Loft Kiosk");
            return "kiosk/error";
        }
    }

    @GetMapping("/success")
    public String bookingSuccess(Model model) {
        model.addAttribute("pageTitle", "Booking Confirmed - Loft Kiosk");
        return "kiosk/success";
    }

    @GetMapping("/reset")
    public String resetKiosk() {
        return "redirect:/kiosk";
    }
}
