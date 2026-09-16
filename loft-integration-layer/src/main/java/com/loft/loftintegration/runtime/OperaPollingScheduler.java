package com.loft.loftintegration.runtime;

import com.loft.loftintegration.sync.SyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers SyncEngine.pollAll() on a fixed interval, configured via
 * loft-integration.poll-interval-seconds in application.yml (default 60s
 * — see application.yml).
 *
 * fixedDelay (not fixedRate) is deliberate: waits for one poll cycle to
 * fully finish before starting the delay countdown for the next one, so
 * a slow poll against Opera's DB can't cause overlapping concurrent
 * polls to pile up.
 */
@Component
public class OperaPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(OperaPollingScheduler.class);

    private final SyncEngine syncEngine;

    public OperaPollingScheduler(SyncEngine syncEngine) {
        this.syncEngine = syncEngine;
    }

    @Scheduled(fixedDelayString = "${loft-integration.poll-interval-seconds}000")
    public void poll() {
        if (!syncEngine.hasRegisteredProperties()) {
            // Nothing registered yet (e.g. property-profile.yml missing) —
            // skip quietly rather than logging a warning every interval.
            return;
        }
        log.debug("Starting scheduled poll cycle.");
        syncEngine.pollAll();
    }
}
