package com.loft.loftintegration.booking.web;

import com.loft.loftintegration.booking.model.RecurringBooking;
import com.loft.loftintegration.booking.service.RecurringBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings/recurring")
public class RecurringBookingController {
    
    private final RecurringBookingService recurringBookingService;
    
    public RecurringBookingController(RecurringBookingService recurringBookingService) {
        this.recurringBookingService = recurringBookingService;
    }
    
    @PostMapping
    public ResponseEntity<RecurringBookingResponse> createRecurringBooking(
            @RequestBody CreateRecurringBookingRequest request) {
        
        RecurringBooking booking = recurringBookingService.createRecurringBooking(
            request.getPropertyCode(),
            request.getActivityTypeId(),
            request.getGuestProfileId(),
            request.getOperaReservationId(),
            request.getDaysOfWeek(),
            request.getStartTime(),
            request.getDurationMinutes(),
            request.getStartDate(),
            request.getEndDate(),
            request.getTotalOccurrences()
        );
        
        return ResponseEntity.ok(toResponse(booking));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RecurringBookingResponse> getRecurringBooking(@PathVariable Long id) {
        RecurringBooking booking = recurringBookingService.getRecurringBooking(id);
        return ResponseEntity.ok(toResponse(booking));
    }
    
    @GetMapping("/property/{propertyCode}")
    public ResponseEntity<List<RecurringBookingResponse>> getActiveRecurringBookings(
            @PathVariable String propertyCode) {
        
        List<RecurringBooking> bookings = recurringBookingService.getActiveRecurringBookings(propertyCode);
        return ResponseEntity.ok(bookings.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelRecurringBooking(@PathVariable Long id) {
        recurringBookingService.cancelRecurringBooking(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/materialize")
    public ResponseEntity<Void> materializeOccurrences(
            @PathVariable Long id,
            @RequestParam LocalDate upToDate) {
        
        recurringBookingService.materializeOccurrences(id, upToDate);
        return ResponseEntity.ok().build();
    }
    
    private RecurringBookingResponse toResponse(RecurringBooking booking) {
        RecurringBookingResponse response = new RecurringBookingResponse();
        response.setId(booking.getId());
        response.setPropertyCode(booking.getPropertyCode());
        response.setActivityTypeId(booking.getActivityType().getId());
        response.setActivityTypeName(booking.getActivityType().getName());
        response.setGuestProfileId(booking.getGuestProfileId());
        response.setOperaReservationId(booking.getOperaReservationId());
        response.setDaysOfWeek(booking.getDaysOfWeek());
        response.setStartTime(booking.getStartTime());
        response.setDurationMinutes(booking.getDurationMinutes());
        response.setStartDate(booking.getStartDate());
        response.setEndDate(booking.getEndDate());
        response.setTotalOccurrences(booking.getTotalOccurrences());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
}
