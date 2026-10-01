package com.loft.loftintegration.property.web;

import com.loft.loftintegration.property.model.PropertyBusinessHours;
import com.loft.loftintegration.property.service.PropertyBusinessHoursService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing per-property business hours.
 */
@RestController
@RequestMapping("/api/properties/{propertyCode}/business-hours")
public class PropertyBusinessHoursController {

    private final PropertyBusinessHoursService businessHoursService;

    public PropertyBusinessHoursController(PropertyBusinessHoursService businessHoursService) {
        this.businessHoursService = businessHoursService;
    }

    /**
     * Get all business hours for a property.
     */
    @GetMapping
    public ResponseEntity<List<PropertyBusinessHours>> getAllBusinessHours(
            @PathVariable String propertyCode) {
        return ResponseEntity.ok(businessHoursService.getAllBusinessHours(propertyCode));
    }

    /**
     * Get business hours for a specific day.
     */
    @GetMapping("/{dayOfWeek}")
    public ResponseEntity<PropertyBusinessHours> getBusinessHoursForDay(
            @PathVariable String propertyCode,
            @PathVariable DayOfWeek dayOfWeek) {
        return ResponseEntity.ok(businessHoursService.getBusinessHours(propertyCode, dayOfWeek));
    }

    /**
     * Set business hours for a specific day.
     */
    @PutMapping("/{dayOfWeek}")
    public ResponseEntity<PropertyBusinessHours> setBusinessHours(
            @PathVariable String propertyCode,
            @PathVariable DayOfWeek dayOfWeek,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime openingTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime closingTime,
            @RequestParam(defaultValue = "true") boolean isOpen) {
        PropertyBusinessHours hours = businessHoursService.setBusinessHours(
                propertyCode, dayOfWeek, openingTime, closingTime, isOpen);
        return ResponseEntity.ok(hours);
    }

    /**
     * Initialize default business hours (9 AM - 6 PM, all days).
     */
    @PostMapping("/initialize-defaults")
    public ResponseEntity<Void> initializeDefaultBusinessHours(@PathVariable String propertyCode) {
        businessHoursService.initializeDefaultBusinessHours(propertyCode);
        return ResponseEntity.ok().build();
    }

    /**
     * Check if property is open at a specific time.
     */
    @GetMapping("/{dayOfWeek}/is-open")
    public ResponseEntity<Map<String, Boolean>> checkIfOpen(
            @PathVariable String propertyCode,
            @PathVariable DayOfWeek dayOfWeek,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        boolean isOpen = businessHoursService.isPropertyOpen(propertyCode, dayOfWeek, time);
        return ResponseEntity.ok(Map.of("isOpen", isOpen));
    }
}
