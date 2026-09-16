package com.loft.loftintegration.reporting.web;

import com.loft.loftintegration.reporting.service.ReportingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only aggregate reports over existing booking/billing data. No
 * date-range filtering yet (see README) — every number here is a
 * running total since the property started using this system, not a
 * period report. A real "this month vs last month" report needs a
 * genuine date-range design decision, not a guess.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/revenue")
    public RevenueSummaryResponse revenue(@RequestParam("propertyCode") String propertyCode) {
        return reportingService.getRevenueSummary(propertyCode);
    }

    @GetMapping("/bookings")
    public BookingsSummaryResponse bookings(@RequestParam("propertyCode") String propertyCode) {
        return reportingService.getBookingsSummary(propertyCode);
    }
}
