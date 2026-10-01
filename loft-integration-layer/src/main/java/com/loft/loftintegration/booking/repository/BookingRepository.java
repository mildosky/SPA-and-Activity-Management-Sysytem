package com.loft.loftintegration.booking.repository;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByPropertyCodeAndStatus(String propertyCode, BookingStatus status);

    List<Booking> findByPropertyCodeOrderByStartTimeDesc(String propertyCode);

    List<Booking> findByPropertyCodeAndStatusOrderByStartTimeDesc(String propertyCode, BookingStatus status);

    List<Booking> findByPropertyCodeAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
            String propertyCode, LocalDateTime from, LocalDateTime to);

    /**
     * Counts non-cancelled bookings grouped by activity type name.
     * Returns Object[] rows of [String activityTypeName, Long count] —
     * mapped into a proper DTO by ReportingService, not exposed raw.
     */
    @Query("SELECT a.name, COUNT(b) FROM Booking b JOIN b.activityType a "
            + "WHERE b.propertyCode = :propertyCode "
            + "AND b.status <> com.loft.loftintegration.booking.model.BookingStatus.CANCELLED "
            + "GROUP BY a.name")
    List<Object[]> countBookingsByActivityType(@Param("propertyCode") String propertyCode);

    long countByActivityTypeAndGuestProfileId(com.loft.loftintegration.booking.model.ActivityType activityType, String guestProfileId);

    /** Bookings linked to one Opera profile — used to resolve a guest's live in-house stay at booking time. */
    List<Booking> findByGuestProfileId(String guestProfileId);
}
