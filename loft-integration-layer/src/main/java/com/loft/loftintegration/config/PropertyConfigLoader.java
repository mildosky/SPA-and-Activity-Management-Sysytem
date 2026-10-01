package com.loft.loftintegration.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads one or more PropertyProfile entries from a YAML file.
 *
 * v1 reads from a local file per install (see property-profile-example.yml).
 * A future admin/webshop module could replace this with a database-backed
 * store without changing PmsConnector or SyncEngine at all — that's the
 * point of keeping this behind a small loader class rather than wiring
 * YAML parsing directly into the connectors.
 *
 * connectionSettings values support ${ENV_VAR_NAME} placeholders,
 * resolved from the real process environment at load time — this is
 * how credentials avoid living in plaintext in the YAML file (see
 * property-profile-example.yml for the recommended pattern). A literal
 * value with no ${...} is still accepted as-is, so this is opt-in, not
 * forced — useful for genuinely non-sensitive fields like host/port.
 * Deliberately environment variables, not a secrets-manager
 * integration (Vault, AWS Secrets Manager, etc.) — this product is
 * installed on-prem at individual properties by IT staff, not deployed
 * into cloud infrastructure with that kind of secrets tooling already
 * in place; env vars are the right complexity level for that
 * deployment model.
 */
public class PropertyConfigLoader {

    private static final Pattern ENV_VAR_PLACEHOLDER = Pattern.compile("^\\$\\{([A-Za-z_][A-Za-z0-9_]*)}$");

    @SuppressWarnings("unchecked")
    public List<PropertyProfile> loadFromFile(String path) throws IOException {
        Yaml yaml = new Yaml(new Constructor(Map.class, new LoaderOptions()));
        try (InputStream in = new FileInputStream(path)) {
            Map<String, Object> root = yaml.load(in);
            List<Map<String, Object>> rawProperties = (List<Map<String, Object>>) root.get("properties");

            return rawProperties.stream().map(this::toProfile).toList();
        }
    }

    private PropertyProfile toProfile(Map<String, Object> raw) {
        PropertyProfile profile = new PropertyProfile();
        profile.setPropertyCode((String) raw.get("propertyCode"));
        profile.setPropertyName((String) raw.get("propertyName"));
        profile.setConnectorId((String) raw.get("connectorId"));
        profile.setLicenseKey((String) raw.get("licenseKey"));

        @SuppressWarnings("unchecked")
        Map<String, String> rawConnectionSettings = (Map<String, String>) raw.get("connectionSettings");
        profile.setConnectionSettings(resolveEnvPlaceholders(rawConnectionSettings, profile.getPropertyCode()));

        return profile;
    }

    /**
     * Resolves any ${ENV_VAR_NAME} value against System.getenv(). Fails
     * fast with a clear, specific message naming the missing variable
     * and which property/setting needed it — not a confusing downstream
     * Oracle auth failure using a literal "${OPERA_DB_PASSWORD}" string
     * as the actual password.
     */
    private Map<String, String> resolveEnvPlaceholders(Map<String, String> connectionSettings, String propertyCode) {
        if (connectionSettings == null) {
            return null;
        }

        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : connectionSettings.entrySet()) {
            resolved.put(entry.getKey(), resolveOneValue(entry.getKey(), entry.getValue(), propertyCode));
        }
        return resolved;
    }

    private String resolveOneValue(String settingName, String rawValue, String propertyCode) {
        if (rawValue == null) {
            return null;
        }

        Matcher matcher = ENV_VAR_PLACEHOLDER.matcher(rawValue.trim());
        if (!matcher.matches()) {
            // Not a ${...} placeholder — a literal value, used as-is.
            return rawValue;
        }

        String envVarName = matcher.group(1);
        String envValue = System.getenv(envVarName);
        if (envValue == null || envValue.isBlank()) {
            throw new IllegalStateException(
                    "property-profile.yml for property '" + propertyCode + "' references environment variable "
                            + "${" + envVarName + "} for connectionSettings." + settingName
                            + ", but that environment variable is not set (or is blank). "
                            + "Set it before starting the app — e.g. in IntelliJ's Run Configuration under "
                            + "'Environment variables', or via 'export " + envVarName + "=...' / "
                            + "'set " + envVarName + "=...' depending on your shell.");
        }
        return envValue;
    }
}
