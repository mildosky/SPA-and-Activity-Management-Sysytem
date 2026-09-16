package com.loft.loftintegration.booking.repository;

import com.loft.loftintegration.booking.model.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {
    List<ActivityType> findByPropertyCode(String propertyCode);
}
