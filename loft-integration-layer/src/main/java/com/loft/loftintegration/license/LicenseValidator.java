package com.loft.loftintegration.license;

import com.loft.loftintegration.config.PropertyProfile;

/**
 * Validates a property's license key before its connector is allowed to
 * start syncing.
 *
 * This is a stub. If you're reusing the LOFT OperaScan licensing scheme
 * (machine-bound SHA-256(REG_CODE|PROPERTY_CODE) fingerprint, keys issued
 * by your private KeyGen tool), port that validation logic in here rather
 * than duplicating a second scheme — keep one licensing system across
 * both products so you only maintain one KeyGen tool and one revocation
 * path.
 */
public class LicenseValidator {

    public boolean isValid(PropertyProfile property) {
        String licenseKey = property.getLicenseKey();
        if (licenseKey == null || licenseKey.isBlank()) {
            return false;
        }

        // TODO: port your existing hardware-fingerprint + property-code
        // validation here instead of this placeholder check.
        return licenseKey.startsWith("LOFT-") && licenseKey.contains(property.getPropertyCode());
    }
}
