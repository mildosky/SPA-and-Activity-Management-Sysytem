package com.loft.loftintegration.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for "which property does this install serve?".
 *
 * Historically the app hard-coded "LOFT" as the property code in every
 * screen and service, while property-profile.yml registers properties by
 * their Opera code (e.g. "TGL"). That mismatch meant charges created with
 * propertyCode "LOFT" could never find the live connector registered under
 * "TGL", producing errors like:
 *   "No live Opera connector is registered for property LOFT"
 *
 * This component resolves the active property code once at startup:
 *   1. If loft-integration.property-code is set explicitly, use it.
 *   2. Otherwise, if exactly one property is registered from
 *      property-profile.yml, use its propertyCode.
 *   3. Otherwise fall back to "LOFT" (fresh installs / dev mode with no
 *      PMS link — same behaviour as before).
 */
@Component
public class LoftProperties {

    private static final Logger log = LoggerFactory.getLogger(LoftProperties.class);

    /** Fallback used when nothing else can be determined (dev/no-PMS installs). */
    public static final String DEFAULT_PROPERTY_CODE = "LOFT";

    private volatile String propertyCode = DEFAULT_PROPERTY_CODE;

    public LoftProperties(org.springframework.core.env.Environment env) {
        String explicit = env.getProperty("loft-integration.property-code");
        if (explicit != null && !explicit.isBlank()) {
            this.propertyCode = explicit.trim();
            log.info("Active property code (from loft-integration.property-code): {}", propertyCode);
        }
    }

    /**
     * Called by OperaSyncStartup after loading property-profile.yml.
     * If no explicit code was configured, adopt the profile's code so the
     * whole app (kiosk, admin, billing, sync) speaks the same property code
     * as Opera.
     */
    public void registerFromProfiles(List<PropertyProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return;
        }
        if (!DEFAULT_PROPERTY_CODE.equals(propertyCode)) {
            // An explicit loft-integration.property-code wins.
            boolean matches = profiles.stream()
                    .anyMatch(p -> propertyCode.equalsIgnoreCase(p.getPropertyCode()));
            if (!matches) {
                log.warn("Configured property-code '{}' does not match any property in "
                        + "property-profile.yml {} — folio posting may not find a connector.",
                        propertyCode,
                        profiles.stream().map(PropertyProfile::getPropertyCode).toList());
            }
            return;
        }
        if (profiles.size() == 1) {
            this.propertyCode = profiles.get(0).getPropertyCode();
            log.info("Adopted property code '{}' from property-profile.yml as the active property.",
                    propertyCode);
        } else {
            log.warn("Multiple properties registered {} — keeping default '{}'. "
                    + "Set loft-integration.property-code to choose one.",
                    profiles.stream().map(PropertyProfile::getPropertyCode).toList(), propertyCode);
        }
    }

    /** The property code all screens/services should use for this install. */
    public String currentPropertyCode() {
        return propertyCode;
    }
}
