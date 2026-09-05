package com.loft.loftintegration.booking.repository;

import com.loft.loftintegration.booking.model.RecurringBooking;
import com.loft.loftintegration.booking.model.RecurringBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringBookingRepository extends JpaRepository<RecurringBooking, Long> {
    
    List<RecurringBooking> findByPropertyCodeAndStatus(String propertyCode, RecurringBookingStatus status);
    
    List<RecurringBooking> findByPropertyCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        String propertyCode, LocalDate startDate, LocalDate endDate);
    
    List<RecurringBooking> findByGuestProfileId(String guestProfileId);
}
