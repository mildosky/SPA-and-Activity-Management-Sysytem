package com.loft.loftintegration.connector;

import com.loft.loftintegration.config.PropertyProfile;
import com.loft.loftintegration.sync.model.FolioEvent;
import com.loft.loftintegration.sync.model.GuestProfileEvent;
import com.loft.loftintegration.sync.model.ReservationEvent;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contract every PMS integration must satisfy.
 *
 * Deliberately narrow: connectors only need to know how to CONNECT to a
 * specific PMS and PULL its native events. All normalization into the
 * product's internal model happens in the sync engine, not here — that
 * keeps connector implementations thin and swappable.
 *
 * Planned implementations:
 *  - OperaV5DirectConnector  (on-prem, direct DB/interface access — v1 target)
 *  - OperaOhipConnector      (Oracle's REST/OAuth2 cloud interface — later)
 */
public interface PmsConnector {

    /**
     * Unique id for this connector type, e.g. "opera-v5-direct", "opera-ohip".
     * Used in property-profile.yml to select which connector a property uses.
     */
    String connectorId();

    /**
     * Open a connection/session against the PMS for the given property.
     * Should be idempotent — safe to call again if the connection drops.
     */
    void connect(PropertyProfile property) throws PmsConnectionException;

    /** Close any open connection/session cleanly. */
    void disconnect();

    /** True if the connector currently has a live session to the PMS. */
    boolean isConnected();

    /**
     * Pull reservation changes (new, modified, cancelled) since the connector's
     * last checkpoint. Checkpointing strategy is connector-specific (e.g. a
     * timestamp column for direct DB access, a webhook cursor for OHIP).
     */
    List<ReservationEvent> pullReservationEvents();

    /** Pull guest profile changes since the last checkpoint. */
    List<GuestProfileEvent> pullGuestProfileEvents();

    /** Pull folio/billing changes since the last checkpoint. */
    List<FolioEvent> pullFolioEvents();

    /**
     * Posts a charge to a guest's folio for a specific reservation — the
     * first WRITE operation this interface exposes, unlike everything
     * else above which only reads. Implementations should throw rather
     * than silently no-op on failure, since a caller (BillingService)
     * needs to know definitively whether the charge landed in the PMS
     * or not, to decide whether to mark it POSTED or keep retrying.
     */
    void postFolioCharge(String reservationId, BigDecimal amount, String currency, String description) throws Exception;
}
