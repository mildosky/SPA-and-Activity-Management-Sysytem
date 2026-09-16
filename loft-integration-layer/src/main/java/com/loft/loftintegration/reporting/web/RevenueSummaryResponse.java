package com.loft.loftintegration.reporting.web;

import java.math.BigDecimal;

/**
 * confirmedRevenue = POSTED_LOCALLY + POSTED_TO_OPERA (money actually
 * billed and settled, regardless of whether it reached Opera's folio).
 * pendingRevenue = charges created but not yet posted anywhere — a
 * PENDING charge only happens if Opera folio posting was enabled but
 * failed (see BillingService); under the default config this will
 * always be zero, which is expected, not a bug.
 */
public record RevenueSummaryResponse(
        String propertyCode,
        BigDecimal confirmedRevenue,
        BigDecimal pendingRevenue,
        BigDecimal voidedRevenue,
        BigDecimal bookingRevenue,
        BigDecimal retailRevenue
) {
}
