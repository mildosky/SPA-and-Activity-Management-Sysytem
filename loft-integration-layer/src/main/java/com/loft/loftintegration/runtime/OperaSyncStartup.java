package com.loft.loftintegration.runtime;

import com.loft.loftintegration.config.PropertyConfigLoader;
import com.loft.loftintegration.config.PropertyProfile;
import com.loft.loftintegration.connector.opera.OperaV5DirectConnector;
import com.loft.loftintegration.sync.SyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Runs once at application startup: loads property-profile.yml, and for
 * each configured property, instantiates the right connector and
 * registers it with SyncEngine.
 *
 * Only "opera-v5-direct" is wired up so far — OperaOhipConnector is still
 * a stub (see its class doc), so any property configured with connectorId
 * "opera-ohip" is skipped with a warning rather than started broken.
 *
 * Missing or empty property-profile.yml is logged as a warning, not a
 * startup failure — lets the app come up cleanly on a fresh install
 * before anyone's filled in real property config yet.
 */
@Component
public class OperaSyncStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperaSyncStartup.class);

    private final SyncEngine syncEngine;
    private final String propertyConfigPath;

    public OperaSyncStartup(
            SyncEngine syncEngine,
            @Value("${loft-integration.property-config-path}") String propertyConfigPath) {
        this.syncEngine = syncEngine;
        this.propertyConfigPath = propertyConfigPath;
    }

    @Override
    public void run(ApplicationArguments args) {
        File configFile = new File(propertyConfigPath);
        if (!configFile.exists()) {
            log.warn("No property-profile.yml found at {} — starting with zero properties registered. "
                    + "Copy src/main/resources/property-profile-example.yml there and fill in real "
                    + "connection details to start syncing.", propertyConfigPath);
            return;
        }

        List<PropertyProfile> properties;
        try {
            properties = new PropertyConfigLoader().loadFromFile(propertyConfigPath);
        } catch (Exception e) {
            log.error("Failed to load property-profile.yml from {} — starting with zero properties registered.",
                    propertyConfigPath, e);
            return;
        }

        if (properties.isEmpty()) {
            log.warn("property-profile.yml at {} loaded but contained no properties.", propertyConfigPath);
            return;
        }

        for (PropertyProfile property : properties) {
            registerProperty(property);
        }
    }

    private void registerProperty(PropertyProfile property) {
        String connectorId = property.getConnectorId();

        if (!"opera-v5-direct".equals(connectorId)) {
            log.warn("Property {} configured with connectorId '{}', which has no working implementation yet "
                    + "(only 'opera-v5-direct' is wired up) — skipping.", property.getPropertyCode(), connectorId);
            return;
        }

        try {
            OperaV5DirectConnector connector = new OperaV5DirectConnector();
            syncEngine.registerProperty(property, connector);
        } catch (Exception e) {
            // A connection failure for one property shouldn't take down
            // the whole application — other properties may still be fine.
            log.error("Failed to register connector for property {} — this property will not sync until fixed.",
                    property.getPropertyCode(), e);
        }
    }
}
