package com.loft.loftintegration.sync;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Remembers Opera's current business date, captured by connectors at
 * connect() time (MAX(BUSINESS_DATE) over FINANCIAL_TRANSACTIONS).
 *
 * Why this exists: the Opera test/lab environments are deliberately
 * backdated (e.g. The George Lagos lab sits on 03/03/2023 while the JVM
 * clock reads the real calendar date). Any "is this stay in-house TODAY?"
 * check driven off LocalDate.now() therefore finds nothing — every lab
 * reservation looks already checked out. Synced state (the in-house
 * mirror, kiosk guest picker, folio-posting resolution) must be judged
 * against Opera's own idea of today, not the system date.
 *
 * Falls back to the system date when no property has connected yet
 * (fresh install, or H2/dev mode with no Opera), so behaviour is
 * unchanged for environments without a live PMS link.
 */
@Component
public class BusinessDateHolder {

    private final AtomicReference<LocalDate> operaBusinessDate = new AtomicReference<>();

    /** Called by connectors/SyncEngine once Opera's business date is resolved. */
    public void set(LocalDate businessDate) {
        if (businessDate != null) {
            operaBusinessDate.set(businessDate);
        }
    }

    /** Last known Opera business date, or null if never captured. */
    public LocalDate peek() {
        return operaBusinessDate.get();
    }

    /**
     * The date all "today" logic should use: Opera's business date when
     * known, otherwise the system date as a safe default.
     */
    public LocalDate today() {
        LocalDate captured = operaBusinessDate.get();
        return captured != null ? captured : LocalDate.now();
    }
}
