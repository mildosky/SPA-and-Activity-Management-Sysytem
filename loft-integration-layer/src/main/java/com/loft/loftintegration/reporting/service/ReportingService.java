package com.loft.loftintegration.reporting.service;

import com.loft.loftintegration.booking.model.BookingStatus;
import com.loft.loftintegration.booking.repository.BookingRepository;
import com.loft.loftintegration.pos.model.ChargeStatus;
import com.loft.loftintegration.pos.model.ChargeType;
import com.loft.loftintegration.pos.repository.ChargeRepository;
import com.loft.loftintegration.reporting.web.BookingsSummaryResponse;
import com.loft.loftintegration.reporting.web.RevenueSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only aggregation over data other modules already collect —
 * deliberately no new domain model here, just queries. Every number
 * this reports is a straightforward reflection of Charge/Booking rows
 * that already exist; nothing here computes or infers anything new.
 */
@Service
public class ReportingService {

    private final ChargeRepository chargeRepository;
    private final BookingRepository bookingRepository;

    public ReportingService(ChargeRepository chargeRepository, BookingRepository bookingRepository) {
        this.chargeRepository = chargeRepository;
        this.bookingRepository = bookingRepository;
    }

    public RevenueSummaryResponse getRevenueSummary(String propertyCode) {
        BigDecimal postedLocally = chargeRepository.sumAmountByPropertyCodeAndStatus(propertyCode, ChargeStatus.POSTED_LOCALLY);
        BigDecimal postedToOpera = chargeRepository.sumAmountByPropertyCodeAndStatus(propertyCode, ChargeStatus.POSTED_TO_OPERA);
        BigDecimal pending = chargeRepository.sumAmountByPropertyCodeAndStatus(propertyCode, ChargeStatus.PENDING);
        BigDecimal voided = chargeRepository.sumAmountByPropertyCodeAndStatus(propertyCode, ChargeStatus.VOIDED);

        BigDecimal bookingRevenue = chargeRepository.sumAmountByPropertyCodeAndChargeType(propertyCode, ChargeType.BOOKING);
        BigDecimal retailRevenue = chargeRepository.sumAmountByPropertyCodeAndChargeType(propertyCode, ChargeType.RETAIL_SALE);

        return new RevenueSummaryResponse(
                propertyCode,
                postedLocally.add(postedToOpera),
                pending,
                voided,
                bookingRevenue,
                retailRevenue
        );
    }

    public BookingsSummaryResponse getBookingsSummary(String propertyCode) {
        long confirmed = bookingRepository.countByPropertyCodeAndStatus(propertyCode, BookingStatus.CONFIRMED);
        long pendingPayment = bookingRepository.countByPropertyCodeAndStatus(propertyCode, BookingStatus.PENDING_PAYMENT);
        long tentative = bookingRepository.countByPropertyCodeAndStatus(propertyCode, BookingStatus.TENTATIVE);
        long cancelled = bookingRepository.countByPropertyCodeAndStatus(propertyCode, BookingStatus.CANCELLED);
        long completed = bookingRepository.countByPropertyCodeAndStatus(propertyCode, BookingStatus.COMPLETED);

        List<BookingsSummaryResponse.ActivityBreakdown> breakdown = bookingRepository
                .countBookingsByActivityType(propertyCode).stream()
                .map(row -> new BookingsSummaryResponse.ActivityBreakdown((String) row[0], (Long) row[1]))
                .toList();

        return new BookingsSummaryResponse(propertyCode, confirmed, pendingPayment, tentative, cancelled, completed, breakdown);
    }
}
