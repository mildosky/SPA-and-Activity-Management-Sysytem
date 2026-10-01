package com.loft.loftintegration.booking.repository;

import com.loft.loftintegration.booking.model.ResourceAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceAssignmentRepository extends JpaRepository<ResourceAssignment, Long> {

    /**
     * Returns the IDs of resources (from the given candidate list) that are
     * already assigned to a non-cancelled booking whose time window
     * overlaps [startTime, endTime). This is the query the whole
     * availability-checking system is built on — everything else in
     * AvailabilityService is just "start with all active resources of the
     * right type, subtract whatever this returns".
     *
     * Overlap test: existing.startTime < newEnd AND existing.endTime > newStart
     * (standard half-open interval overlap check).
     */
    @Query("""
            SELECT DISTINCT ra.resource.id
            FROM ResourceAssignment ra
            WHERE ra.resource.id IN :resourceIds
              AND ra.booking.status != com.loft.loftintegration.booking.model.BookingStatus.CANCELLED
              AND ra.booking.startTime < :endTime
              AND ra.booking.endTime > :startTime
            """)
    List<Long> findBusyResourceIds(
            @Param("resourceIds") List<Long> resourceIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
