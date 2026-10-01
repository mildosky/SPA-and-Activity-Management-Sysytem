package com.loft.loftintegration.connector.opera;

import com.loft.loftintegration.config.PropertyProfile;
import com.loft.loftintegration.connector.PmsConnectionException;
import com.loft.loftintegration.connector.PmsConnector;
import com.loft.loftintegration.sync.model.FolioEvent;
import com.loft.loftintegration.sync.model.GuestProfileEvent;
import com.loft.loftintegration.sync.model.ReservationEvent;

import java.util.List;

/**
 * Placeholder connector for Oracle's OHIP (OPERA Hospitality Integration
 * Platform) REST/OAuth2 interface — the direction Oracle is pushing
 * customers toward for newer/cloud OPERA installs.
 *
 * Not needed for v1 (your target customers are on-prem Opera v5), but
 * the interface contract is defined now so adding this later doesn't
 * require touching PmsConnector, SyncEngine, or any downstream module.
 *
 * When you build this out for real: OHIP uses OAuth2 client-credentials
 * auth and exposes reservation/profile/folio data as REST resources —
 * you'll swap the JDBC logic here for an HTTP client (webflux is already
 * on the classpath) and a token-refresh cycle instead of a live DB session.
 */
public class OperaOhipConnector implements PmsConnector {

    @Override
    public String connectorId() {
        return "opera-ohip";
    }

    @Override
    public void connect(PropertyProfile property) throws PmsConnectionException {
        throw new PmsConnectionException("OHIP connector not yet implemented.");
    }

    @Override
    public void disconnect() {
        // no-op until implemented
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public List<ReservationEvent> pullReservationEvents() {
        throw new UnsupportedOperationException("OHIP connector not yet implemented.");
    }

    @Override
    public List<GuestProfileEvent> pullGuestProfileEvents() {
        throw new UnsupportedOperationException("OHIP connector not yet implemented.");
    }

    @Override
    public List<FolioEvent> pullFolioEvents() {
        throw new UnsupportedOperationException("OHIP connector not yet implemented.");
    }

    @Override
    public void postFolioCharge(String reservationId, java.math.BigDecimal amount, String currency, String description) {
        throw new UnsupportedOperationException("OHIP connector not yet implemented.");
    }
}
