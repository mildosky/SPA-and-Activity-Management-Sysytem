package com.loft.loftintegration.pos.model;

public enum ChargeStatus {
    /** Not yet posted anywhere beyond this system's own database. */
    PENDING,
    /** Successfully posted to Opera's FINANCIAL_TRANSACTIONS. */
    POSTED_TO_OPERA,
    /** Finalized locally without an Opera post — no linked reservation, or posting is disabled/failed permanently. */
    POSTED_LOCALLY,
    VOIDED
}
