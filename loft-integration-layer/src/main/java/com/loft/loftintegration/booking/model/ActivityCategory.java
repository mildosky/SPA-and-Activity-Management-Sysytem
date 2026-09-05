package com.loft.loftintegration.booking.model;

/**
 * Broad category an ActivityType falls under. This exists mainly for
 * filtering/display purposes (e.g. "show me all treatments" in a future
 * webshop UI) — it doesn't drive scheduling logic. The actual scheduling
 * behavior for any activity, regardless of category, comes entirely
 * from its ResourceRequirements. A golf tee time and a spa treatment
 * are scheduled by the exact same code path.
 */
public enum ActivityCategory {
    TREATMENT,
    FITNESS_CLASS,
    RACQUET_SPORT,
    GOLF,
    GENERIC_ACTIVITY
}
