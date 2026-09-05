package com.loft.loftintegration.booking.service;

import com.loft.loftintegration.booking.model.*;
import com.loft.loftintegration.booking.repository.RecurringBookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RecurringBookingService {
    
    private static final Logger log = LoggerFactory.getLogger(RecurringBookingService.class);
    
    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingService bookingService;
    private final AvailabilityService availabilityService;
    
    public RecurringBookingService(
        RecurringBookingRepository recurringBookingRepository,
        BookingService bookingService,
        AvailabilityService availabilityService) {
        this.recurringBookingRepository = recurringBookingRepository;
        this.bookingService = bookingService;
        this.availabilityService = availabilityService;
    }
    
    /**
     * Create a new recurring booking pattern.
     * This creates the pattern but does not create individual bookings yet.
     */
    @Transactional
    public RecurringBooking createRecurringBooking(
        String propertyCode,
        Long activityTypeId,
        String guestProfileId,
        String operaReservationId,
        Set<DayOfWeek> daysOfWeek,
        LocalTime startTime,
        int durationMinutes,
        LocalDate startDate,
        LocalDate endDate,
        Integer totalOccurrences) {
        
        ActivityType activityType = bookingService.getActivityType(activityTypeId);
        
        RecurringBooking recurringBooking = new RecurringBooking(
            propertyCode,
            activityType,
            guestProfileId,
            operaReservationId,
            daysOfWeek,
            startTime,
            durationMinutes,
            startDate,
            endDate,
            totalOccurrences
        );
        
        return recurringBookingRepository.save(recurringBooking);
    }
    
    /**
     * Generate individual bookings from a recurring booking pattern up to a given date.
     * This should be called periodically (e.g., daily) to materialize future occurrences.
     */
    @Transactional
    public List<Booking> materializeOccurrences(Long recurringBookingId, LocalDate upToDate) {
        RecurringBooking recurringBooking = recurringBookingRepository.findById(recurringBookingId)
            .orElseThrow(() -> new IllegalArgumentException("Recurring booking not found: " + recurringBookingId));
        
        if (recurringBooking.getStatus() != RecurringBookingStatus.ACTIVE) {
            log.warn("Cannot materialize occurrences for inactive recurring booking: {}", recurringBookingId);
            return List.of();
        }
        
        List<Booking> createdBookings = new ArrayList<>();
        LocalDate currentDate = recurringBooking.getStartDate();
        LocalDate limitDate = (recurringBooking.getEndDate() != null) 
            ? recurringBooking.getEndDate().minusDays(1) 
            : upToDate;
        
        if (recurringBooking.getTotalOccurrences() != null) {
            // Count existing occurrences to avoid exceeding total
            long existingCount = countExistingOccurrences(recurringBooking);
            if (existingCount >= recurringBooking.getTotalOccurrences()) {
                log.info("Recurring booking {} has reached total occurrences limit", recurringBookingId);
                return List.of();
            }
        }
        
        while (!currentDate.isAfter(limitDate) && !currentDate.isAfter(upToDate)) {
            // Check if we've reached the total occurrences limit
            if (recurringBooking.getTotalOccurrences() != null && 
                createdBookings.size() + countExistingOccurrences(recurringBooking) >= recurringBooking.getTotalOccurrences()) {
                break;
            }
            
            if (recurringBooking.getDaysOfWeek().contains(currentDate.getDayOfWeek())) {
                LocalDateTime startDateTime = LocalDateTime.of(currentDate, recurringBooking.getStartTime());
                LocalDateTime endDateTime = startDateTime.plusMinutes(recurringBooking.getDurationMinutes());
                
                try {
                    // Try to create a booking for this occurrence
                    Booking booking = bookingService.createBooking(
                        recurringBooking.getPropertyCode(),
                        recurringBooking.getActivityType().getId(),
                        recurringBooking.getGuestProfileId(),
                        recurringBooking.getOperaReservationId(),
                        startDateTime,
                        endDateTime
                    );
                    createdBookings.add(booking);
                    log.info("Materialized booking {} from recurring pattern {}", booking.getId(), recurringBookingId);
                } catch (InsufficientAvailabilityException e) {
                    log.warn("Could not materialize booking for {}: {}", currentDate, e.getMessage());
                    // Continue with next date even if this one fails
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return createdBookings;
    }
    
    private long countExistingOccurrences(RecurringBooking recurringBooking) {
        // Count bookings that match this recurring pattern
        // This is a simplified check - in production you might want a more robust tracking mechanism
        return bookingService.countBookingsForRecurringPattern(recurringBooking);
    }
    
    /**
     * Cancel a recurring booking pattern.
     * This marks the pattern as cancelled but does not delete existing bookings.
     */
    @Transactional
    public void cancelRecurringBooking(Long recurringBookingId) {
        RecurringBooking recurringBooking = recurringBookingRepository.findById(recurringBookingId)
            .orElseThrow(() -> new IllegalArgumentException("Recurring booking not found: " + recurringBookingId));
        
        recurringBooking.setStatus(RecurringBookingStatus.CANCELLED);
        recurringBookingRepository.save(recurringBooking);
        
        log.info("Cancelled recurring booking pattern {}", recurringBookingId);
    }
    
    /**
     * Complete a recurring booking pattern when all occurrences are done.
     */
    @Transactional
    public void completeRecurringBooking(Long recurringBookingId) {
        RecurringBooking recurringBooking = recurringBookingRepository.findById(recurringBookingId)
            .orElseThrow(() -> new IllegalArgumentException("Recurring booking not found: " + recurringBookingId));
        
        recurringBooking.setStatus(RecurringBookingStatus.COMPLETED);
        recurringBookingRepository.save(recurringBooking);
        
        log.info("Completed recurring booking pattern {}", recurringBookingId);
    }
    
    public List<RecurringBooking> getActiveRecurringBookings(String propertyCode) {
        return recurringBookingRepository.findByPropertyCodeAndStatus(propertyCode, RecurringBookingStatus.ACTIVE);
    }
    
    public RecurringBooking getRecurringBooking(Long id) {
        return recurringBookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring booking not found: " + id));
    }
}
