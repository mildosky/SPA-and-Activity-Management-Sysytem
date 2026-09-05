package com.loft.loftintegration.booking.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public class CreateRecurringBookingRequest {
    
    private String propertyCode;
    private Long activityTypeId;
    private String guestProfileId;
    private String operaReservationId;
    private Set<DayOfWeek> daysOfWeek;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    private int durationMinutes;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private Integer totalOccurrences;

    public String getPropertyCode() { return propertyCode; }
    public void setPropertyCode(String propertyCode) { this.propertyCode = propertyCode; }
    
    public Long getActivityTypeId() { return activityTypeId; }
    public void setActivityTypeId(Long activityTypeId) { this.activityTypeId = activityTypeId; }
    
    public String getGuestProfileId() { return guestProfileId; }
    public void setGuestProfileId(String guestProfileId) { this.guestProfileId = guestProfileId; }
    
    public String getOperaReservationId() { return operaReservationId; }
    public void setOperaReservationId(String operaReservationId) { this.operaReservationId = operaReservationId; }
    
    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public Integer getTotalOccurrences() { return totalOccurrences; }
    public void setTotalOccurrences(Integer totalOccurrences) { this.totalOccurrences = totalOccurrences; }
}
