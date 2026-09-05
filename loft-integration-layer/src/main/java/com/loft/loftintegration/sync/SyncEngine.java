package com.loft.loftintegration.sync;

import com.loft.loftintegration.config.PropertyProfile;
import com.loft.loftintegration.connector.PmsConnectionException;
import com.loft.loftintegration.connector.PmsConnector;
import com.loft.loftintegration.directory.service.GuestDirectoryService;
import com.loft.loftintegration.sync.consumer.FolioEventConsumer;
import com.loft.loftintegration.sync.consumer.ReservationEventConsumer;
import com.loft.loftintegration.sync.model.FolioEvent;
import com.loft.loftintegration.sync.model.GuestProfileEvent;
import com.loft.loftintegration.sync.model.ReservationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives one PmsConnector per configured property, pulls normalized events
 * on a poll cycle, and hands them off to downstream consumers.
 *
 * Guest profile events now do real work — see publish(GuestProfileEvent)
 * below — feeding GuestDirectoryService's local mirror, which the
 * webshop module matches against to link bookings to real Opera
 * guests. Reservation and folio events are now also wired to real
 * consumers (ReservationEventConsumer, FolioEventConsumer) that drive
 * actual booking and POS operations based on PMS activity.
 *
 * @Component makes this a Spring-managed singleton bean, so
 * OperaSyncStartup and OperaPollingScheduler can both get the same
 * instance injected rather than each constructing their own.
 */
@Component
public class SyncEngine {

    private static final Logger log = LoggerFactory.getLogger(SyncEngine.class);

    private final Map<String, PmsConnector> connectorsByProperty = new ConcurrentHashMap<>();
    private final GuestDirectoryService guestDirectoryService;
    private final ReservationEventConsumer reservationEventConsumer;
    private final FolioEventConsumer folioEventConsumer;

    public SyncEngine(GuestDirectoryService guestDirectoryService,
                      ReservationEventConsumer reservationEventConsumer,
                      FolioEventConsumer folioEventConsumer) {
        this.guestDirectoryService = guestDirectoryService;
        this.reservationEventConsumer = reservationEventConsumer;
        this.folioEventConsumer = folioEventConsumer;
    }

    /** Registers and connects a connector for one property. Call once per configured property at startup. */
    public void registerProperty(PropertyProfile property, PmsConnector connector) throws PmsConnectionException {
        connector.connect(property);
        connectorsByProperty.put(property.getPropertyCode(), connector);
        log.info("Connected {} for property {}", connector.connectorId(), property.getPropertyCode());
    }

    /**
     * Runs one poll cycle across all registered properties. Intended to be
     * called on a schedule (e.g. Spring @Scheduled every 30–60s) rather
     * than continuously — Opera v5 direct DB polling should be gentle on
     * the source database.
     */
    public void pollAll() {
        connectorsByProperty.forEach((propertyCode, connector) -> {
            try {
                List<ReservationEvent> reservations = connector.pullReservationEvents();
                List<GuestProfileEvent> profiles = connector.pullGuestProfileEvents();
                List<FolioEvent> folios = connector.pullFolioEvents();

                reservations.forEach(this::publish);
                profiles.forEach(this::publish);
                folios.forEach(this::publish);
            } catch (Exception e) {
                log.warn("Poll failed for property {}", propertyCode, e);
            }
        });
    }

    /** True once at least one property has been registered — lets the scheduler skip empty poll cycles. */
    public boolean hasRegisteredProperties() {
        return !connectorsByProperty.isEmpty();
    }

    /** True if a live connector is registered for this specific property. */
    public boolean hasProperty(String propertyCode) {
        return connectorsByProperty.containsKey(propertyCode);
    }

    /**
     * Posts a folio charge through the registered connector for this
     * property, reusing its already-open connection rather than opening
     * a new one. Throws PmsConnectionException wrapping the underlying
     * cause on failure — callers (BillingService) need a definitive
     * yes/no on whether the charge actually landed in the PMS.
     */
    public void postFolioCharge(String propertyCode, String reservationId,
                                 java.math.BigDecimal amount, String currency, String description)
            throws PmsConnectionException {
        PmsConnector connector = connectorsByProperty.get(propertyCode);
        if (connector == null) {
            throw new PmsConnectionException("No connector registered for property " + propertyCode);
        }
        try {
            connector.postFolioCharge(reservationId, amount, currency, description);
        } catch (Exception e) {
            throw new PmsConnectionException(
                    "Failed to post folio charge for property " + propertyCode
                            + ", reservation " + reservationId, e);
        }
    }

    private void publish(ReservationEvent event) {
        log.info("Reservation event: {} {}", event.getChangeType(), event.getReservationId());
        reservationEventConsumer.consume(event);
    }

    private void publish(GuestProfileEvent event) {
        log.info("Guest profile event: {} {}", event.getChangeType(), event.getGuestProfileId());
        guestDirectoryService.upsert(event);
    }

    private void publish(FolioEvent event) {
        log.info("Folio event: {} {}", event.getEventType(), event.getFolioId());
        folioEventConsumer.consume(event);
    }
}
