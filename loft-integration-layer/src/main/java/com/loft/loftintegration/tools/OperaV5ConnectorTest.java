package com.loft.loftintegration.tools;

import com.loft.loftintegration.config.PropertyProfile;
import com.loft.loftintegration.connector.opera.OperaV5DirectConnector;
import com.loft.loftintegration.sync.model.FolioEvent;
import com.loft.loftintegration.sync.model.GuestProfileEvent;
import com.loft.loftintegration.sync.model.ReservationEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone end-to-end test of OperaV5DirectConnector — NOT part of the
 * Spring Boot application. Connects for real, pulls each event type once,
 * and prints what comes back so you can eyeball whether the mapping looks
 * right against real George Lagos data.
 *
 * connect() now anchors the checkpoint to Opera's own business date
 * (derived from MAX(BUSINESS_DATE) in FINANCIAL_TRANSACTIONS) rather
 * than the JVM's real-world clock — correct even in this deliberately
 * backdated lab. But that still only looks 1 day back from Opera's
 * business date, and we want to see ALL of this lab's 2009–2024 test
 * data, so this test further widens the checkpoint after connecting.
 */
public class OperaV5ConnectorTest {

    /** Far enough back to cover this lab's backdated 2009–2024 test data. */
    private static final java.time.LocalDateTime CHECKPOINT_CUTOFF =
            java.time.LocalDateTime.of(2000, 1, 1, 0, 0);

    public static void main(String[] args) throws Exception {
        PropertyProfile property = new PropertyProfile();
        property.setPropertyCode("TGL");
        property.setPropertyName("The George Lagos");
        property.setConnectorId("opera-v5-direct");

        Map<String, String> settings = new HashMap<>();
        settings.put("host", "192.168.56.3");
        settings.put("port", "1521");
        settings.put("sid", "OPERA");
        settings.put("username", "opera");
        settings.put("password", "opera");
        property.setConnectionSettings(settings);

        OperaV5DirectConnector connector = new OperaV5DirectConnector();
        connector.connect(property);
        System.out.println("Connected. Setting checkpoint to " + CHECKPOINT_CUTOFF + " for this test run...");

        // Reach into the connector's default checkpoint via a fresh pull —
        // simplest way to widen it for this test without adding a public
        // setter that the real sync engine shouldn't need.
        widenCheckpoint(connector);

        System.out.println();
        System.out.println("=== Reservation events ===");
        List<ReservationEvent> reservations = connector.pullReservationEvents();
        System.out.println("Found " + reservations.size() + " reservation events.");
        reservations.stream().limit(10).forEach(e ->
                System.out.println("  " + e.getChangeType() + " resv=" + e.getReservationId()
                        + " guest=" + e.getGuestProfileId() + " property=" + e.getPropertyCode()
                        + " arrival=" + e.getArrivalDate() + " departure=" + e.getDepartureDate()
                        + " room=" + e.getRoomNumber()));

        System.out.println();
        System.out.println("=== Guest profile events ===");
        List<GuestProfileEvent> profiles = connector.pullGuestProfileEvents();
        System.out.println("Found " + profiles.size() + " guest profile events.");
        profiles.stream().limit(10).forEach(e ->
                System.out.println("  " + e.getChangeType() + " name=" + e.getGuestProfileId()
                        + " " + e.getFirstName() + " " + e.getLastName()
                        + " email=" + e.getEmail() + " phone=" + e.getPhone()));

        System.out.println();
        System.out.println("=== Folio events ===");
        List<FolioEvent> folios = connector.pullFolioEvents();
        System.out.println("Found " + folios.size() + " folio events.");
        folios.stream().limit(10).forEach(e ->
                System.out.println("  " + e.getEventType() + " folio=" + e.getFolioId()
                        + " resv=" + e.getReservationId() + " amount=" + e.getAmount()
                        + " " + e.getCurrency() + " desc=" + e.getDescription()));

        connector.disconnect();
        System.out.println();
        System.out.println("Done.");
    }

    private static void widenCheckpoint(OperaV5DirectConnector connector) throws Exception {
        for (String fieldName : new String[]{"reservationCheckpoint", "guestProfileCheckpoint", "folioCheckpoint"}) {
            var field = OperaV5DirectConnector.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(connector, CHECKPOINT_CUTOFF);
        }
    }
}
