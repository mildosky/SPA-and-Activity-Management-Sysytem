package com.loft.loftintegration.property.repository;

import com.loft.loftintegration.property.model.PropertyBusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyBusinessHoursRepository extends JpaRepository<PropertyBusinessHours, Long> {
    
    List<PropertyBusinessHours> findByPropertyCode(String propertyCode);
    
    Optional<PropertyBusinessHours> findByPropertyCodeAndDayOfWeek(String propertyCode, DayOfWeek dayOfWeek);
}
