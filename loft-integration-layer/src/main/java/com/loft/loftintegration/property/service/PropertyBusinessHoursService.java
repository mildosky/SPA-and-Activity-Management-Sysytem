package com.loft.loftintegration.property.service;

import com.loft.loftintegration.property.model.PropertyBusinessHours;
import com.loft.loftintegration.property.repository.PropertyBusinessHoursRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Service for managing per-property business hours configuration.
 */
@Service
public class PropertyBusinessHoursService {

    private static final Logger log = LoggerFactory.getLogger(PropertyBusinessHoursService.class);

    private final PropertyBusinessHoursRepository repository;

    public PropertyBusinessHoursService(PropertyBusinessHoursRepository repository) {
        this.repository = repository;
    }

    /**
     * Sets business hours for a specific property and day of week.
     */
    @Transactional
    public PropertyBusinessHours setBusinessHours(String propertyCode, DayOfWeek dayOfWeek,
                                                   LocalTime openingTime, LocalTime closingTime, boolean isOpen) {
        PropertyBusinessHours hours = repository
                .findByPropertyCodeAndDayOfWeek(propertyCode, dayOfWeek)
                .orElse(new PropertyBusinessHours(propertyCode, dayOfWeek, openingTime, closingTime, isOpen));
        
        if (!isOpen) {
            hours.setOpen(false);
        } else {
            hours.setOpeningTime(openingTime);
            hours.setClosingTime(closingTime);
            hours.setOpen(true);
        }
        
        log.info("Set business hours for property {} on {}: {} - {}", 
                propertyCode, dayOfWeek, openingTime, closingTime);
        
        return repository.save(hours);
    }

    /**
     * Gets business hours for a specific property and day of week.
     */
    public PropertyBusinessHours getBusinessHours(String propertyCode, DayOfWeek dayOfWeek) {
        return repository.findByPropertyCodeAndDayOfWeek(propertyCode, dayOfWeek)
                .orElseThrow(() -> new NoSuchElementException(
                        "No business hours configured for property " + propertyCode + " on " + dayOfWeek));
    }

    /**
     * Gets all business hours for a property.
     */
    public List<PropertyBusinessHours> getAllBusinessHours(String propertyCode) {
        return repository.findByPropertyCode(propertyCode);
    }

    /**
     * Gets business hours as a map for easy lookup.
     */
    public Map<DayOfWeek, PropertyBusinessHours> getBusinessHoursMap(String propertyCode) {
        return getAllBusinessHours(propertyCode).stream()
                .collect(Collectors.toMap(
                        PropertyBusinessHours::getDayOfWeek,
                        hours -> hours
                ));
    }

    /**
     * Checks if a property is open at a given day and time.
     */
    public boolean isPropertyOpen(String propertyCode, DayOfWeek dayOfWeek, LocalTime time) {
        return getBusinessHours(propertyCode, dayOfWeek).isWithinBusinessHours(time);
    }

    /**
     * Initializes default business hours for a property (9 AM - 6 PM, Mon-Sun).
     */
    @Transactional
    public void initializeDefaultBusinessHours(String propertyCode) {
        LocalTime defaultOpen = LocalTime.of(9, 0);
        LocalTime defaultClose = LocalTime.of(18, 0);
        
        for (DayOfWeek day : DayOfWeek.values()) {
            setBusinessHours(propertyCode, day, defaultOpen, defaultClose, true);
        }
        
        log.info("Initialized default business hours for property {}", propertyCode);
    }
}
