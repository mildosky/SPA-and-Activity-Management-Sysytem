package com.loft.loftintegration.booking.repository;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByPropertyCodeAndStatus(String propertyCode, BookingStatus status);

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
}
