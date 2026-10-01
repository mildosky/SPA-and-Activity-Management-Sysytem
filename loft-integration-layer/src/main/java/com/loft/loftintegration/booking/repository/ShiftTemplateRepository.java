package com.loft.loftintegration.booking.repository;

import com.loft.loftintegration.booking.model.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {
    List<ShiftTemplate> findByResourceId(Long resourceId);

    /** Used by the seed guard to check independently of BookingSeedData's own guard. */
    long countByResourcePropertyCode(String propertyCode);
}
