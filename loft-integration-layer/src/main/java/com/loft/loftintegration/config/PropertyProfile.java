package com.loft.loftintegration.config;

import java.util.Map;

/**
 * Configuration for a single hotel property/customer.
 *
 * Mirrors the property-code-driven pattern used by LOFT OperaScan's
 * licensing — each customer install is identified by a property code
 * and carries its own connection details. connectorId selects which
 * PmsConnector implementation handles this property (e.g. "opera-v5-direct").
 */
public class PropertyProfile {

    private String propertyCode;
    private String propertyName;
    private String connectorId;

    /**
     * Connector-specific connection settings, e.g. for opera-v5-direct:
     * host, port, sid, schema, username, password (or a secrets reference —
     * see README for guidance on not storing plaintext credentials in
     * property-profile.yml for production installs).
     */
    private Map<String, String> connectionSettings;

    /** Licensing key for this install, same format as OperaScan's LOFT-* keys. */
    private String licenseKey;

    public PropertyProfile() {
    }

    public String getPropertyCode() { return propertyCode; }
    public void setPropertyCode(String propertyCode) { this.propertyCode = propertyCode; }

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public String getConnectorId() { return connectorId; }
    public void setConnectorId(String connectorId) { this.connectorId = connectorId; }

    public Map<String, String> getConnectionSettings() { return connectionSettings; }
    public void setConnectionSettings(Map<String, String> connectionSettings) { this.connectionSettings = connectionSettings; }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
}
