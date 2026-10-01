package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.booking.model.ActivityType;
import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.service.BookingService;
import com.loft.loftintegration.booking.web.CreateBookingRequest;
import com.loft.loftintegration.directory.model.InHouseReservationMirror;
import com.loft.loftintegration.directory.repository.OperaGuestMirrorRepository;
import com.loft.loftintegration.directory.service.InHouseReservationService;
import com.loft.loftintegration.pos.model.Charge;
import com.loft.loftintegration.pos.service.BillingService;
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
 *
 * Guests are identified against REAL Opera profiles (synced into the local
 * guest mirror by SyncEngine) — that link is what allows the resulting charge
 * to be posted onto the guest's Opera room folio. A guest who isn't in Opera
 * can still book as a walk-in, but their charge bills locally only.
 */
@Controller
@RequestMapping("/kiosk")
public class KioskController {

    private final com.loft.loftintegration.config.LoftProperties loftProperties;
    /** The property this install serves (adopted from property-profile.yml at startup). */
    private String PROPERTY_CODE = com.loft.loftintegration.config.LoftProperties.DEFAULT_PROPERTY_CODE;

    private final BookingService bookingService;
    private final BillingService billingService;
    private final InHouseReservationService inHouseReservationService;
    private final OperaGuestMirrorRepository guestMirrorRepository;
    private final com.loft.loftintegration.sync.BusinessDateHolder businessDateHolder;

    public KioskController(BookingService bookingService,
                           BillingService billingService,
                           InHouseReservationService inHouseReservationService,
                           OperaGuestMirrorRepository guestMirrorRepository,
                           com.loft.loftintegration.sync.BusinessDateHolder businessDateHolder,
                           com.loft.loftintegration.config.LoftProperties loftProperties) {
        this.bookingService = bookingService;
        this.billingService = billingService;
        this.inHouseReservationService = inHouseReservationService;
        this.guestMirrorRepository = guestMirrorRepository;
        this.businessDateHolder = businessDateHolder;
        this.loftProperties = loftProperties;
    }

    @jakarta.annotation.PostConstruct
    void resolvePropertyCode() {
        this.PROPERTY_CODE = loftProperties.currentPropertyCode();
    }

    @GetMapping
    public String kioskHome(Model model) {
        List<ActivityType> activities = bookingService.listActivityTypes(PROPERTY_CODE);
        model.addAttribute("activities", activities);
        model.addAttribute("pageTitle", "Loft Kiosk");
        return "kiosk/home";
    }

    @GetMapping("/activity/{id}")
    public String selectActivity(@PathVariable Long id, Model model) {
        ActivityType activity = bookingService.getActivityType(id);

        // Date picker starts at Opera's business date, not the system date —
        // lab/test Opera environments are backdated (e.g. 03/03/2023), and
        // availability slots must line up with the PMS's notion of "today".
        LocalDate today = businessDateHolder.today();
        List<LocalDate> next7Days = java.util.stream.IntStream.range(0, 7)
            .mapToObj(i -> today.plusDays(i))
            .toList();

        // Guests we can attach this booking to: real Opera profiles, with their
        // current stay/room from the in-house mirror when they have one.
        java.util.Map<String, String> inHouseRooms = new java.util.HashMap<>();
        for (InHouseReservationMirror m : inHouseReservationService.findInHouseGuests(PROPERTY_CODE)) {
            inHouseRooms.put(m.getGuestProfileId(), m.getRoomNumber());
        }
        model.addAttribute("activity", activity);
        model.addAttribute("dates", next7Days);
        model.addAttribute("dateFormat", DateTimeFormatter.ofPattern("EEE, MMM d"));
        model.addAttribute("inHouseRooms", inHouseRooms);
        model.addAttribute("allProfiles", guestMirrorRepository.findAll());
        model.addAttribute("pageTitle", "Select Date - Loft Kiosk");
        return "kiosk/select-date";
    }

    @GetMapping("/activity/{id}/book")
    public String bookActivity(@PathVariable Long id,
                               @RequestParam String date,
                               @RequestParam String time,
                               @RequestParam(required = false) String profileId,
                               Model model) {
        ActivityType activity = bookingService.getActivityType(id);

        // Normalize the incoming time (e.g. "9:00") to ISO-8601 ("09:00") so that
        // LocalDateTime.parse never fails on single-digit hours.
        LocalTime startTime = LocalTime.parse(time.length() == 4 ? "0" + time : time);
        LocalDateTime startDateTime = LocalDate.parse(date).atTime(startTime);

        // If the guest picked a real Opera profile, show their identity for
        // confirmation and prefill the name — no manual typing needed.
        String matchedName = null;
        String matchedRoom = null;
        if (profileId != null && !profileId.isBlank()) {
            matchedName = guestMirrorRepository.findByOperaNameId(profileId)
                    .map(m -> ((m.getFirstName() == null ? "" : m.getFirstName() + " ")
                            + (m.getLastName() == null ? "" : m.getLastName())).trim())
                    .orElse(null);
            matchedRoom = inHouseReservationService.findByGuestProfileId(profileId)
                    .map(InHouseReservationMirror::getRoomNumber)
                    .orElse(null);
        }

        model.addAttribute("activity", activity);
        model.addAttribute("dateTime", startDateTime);
        model.addAttribute("date", date);
        model.addAttribute("time", time);
        model.addAttribute("selectedProfileId", profileId == null ? "" : profileId);
        model.addAttribute("matchedName", matchedName == null ? "" : matchedName);
        model.addAttribute("matchedRoom", matchedRoom == null ? "" : matchedRoom);
        model.addAttribute("pageTitle", "Guest Information - Loft Kiosk");
        return "kiosk/guest-info";
    }

    @PostMapping("/confirm")
    public String confirmBooking(@RequestParam Long activityId,
                                 @RequestParam String dateTime,
                                 @RequestParam String guestName,
                                 @RequestParam(required = false) String guestEmail,
                                 @RequestParam(required = false) String guestPhone,
                                 @RequestParam(required = false) String operaProfileId,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            ActivityType activity = bookingService.getActivityType(activityId);

            LocalDateTime startDateTime = LocalDateTime.parse(dateTime);
            LocalDateTime endDateTime = startDateTime.plusMinutes(activity.getDefaultDurationMinutes());

            // Real Opera profile if the guest was identified from the mirror;
            // otherwise a synthetic id so staff can still see it's an anonymous
            // walk-in (charges for these bill locally only — no folio exists).
            String profileId = (operaProfileId != null && !operaProfileId.isBlank())
                    ? operaProfileId.trim()
                    : "WEB-KIOSK-" + System.currentTimeMillis();

            // Resolve the guest's CURRENT in-house stay so the booking carries the
            // exact RESV_NAME_ID its charge will post against. Null when the guest
            // isn't checked in — BillingService then falls back to local billing.
            String operaReservationId = inHouseReservationService.findByGuestProfileId(profileId)
                    .map(InHouseReservationMirror::getOperaReservationId)
                    .orElse(null);

            CreateBookingRequest request = new CreateBookingRequest(
                PROPERTY_CODE,
                activityId,
                profileId,
                operaReservationId,
                startDateTime,
                endDateTime,
                guestName,
                guestEmail != null ? guestEmail : "kiosk@loft.com",
                guestPhone,
                "Kiosk booking"
            );

            Booking booking = bookingService.createBooking(
                request.propertyCode(),
                request.activityTypeId(),
                request.guestProfileId(),
                request.operaReservationId(),
                guestName,
                request.startTime(),
                request.endTime()
            );

            // Bill immediately — same as internal staff bookings (the kiosk sits
            // in the spa/reception, payment happens at the desk or on the folio).
            Charge charge = billingService.chargeForBooking(booking);

            boolean linked = operaReservationId != null;
            redirectAttributes.addFlashAttribute("successMessage", "Booking confirmed!");
            redirectAttributes.addFlashAttribute("guestName", guestName);
            redirectAttributes.addFlashAttribute("activityName", activity.getName());
            redirectAttributes.addFlashAttribute("bookingTime", startDateTime);
            redirectAttributes.addFlashAttribute("folioNote",
                    linked ? "Charge of " + charge.getAmount() + " " + charge.getCurrency()
                            + " linked to your Opera folio (reservation " + operaReservationId + ")."
                            : "Charge of " + charge.getAmount() + " " + charge.getCurrency()
                                    + " will be settled at reception (no in-house Opera stay found).");
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
