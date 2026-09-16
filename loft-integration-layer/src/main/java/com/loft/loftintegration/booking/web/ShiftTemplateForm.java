package com.loft.loftintegration.booking.web;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * DTO for creating or updating a ShiftTemplate via the admin UI.
 */
public class ShiftTemplateForm {
    private Long id;
    private Long resourceId;
    private String name;
    private Set<String> daysOfWeek;
    private String startTime;
    private String endTime;

    public ShiftTemplateForm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(Set<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
