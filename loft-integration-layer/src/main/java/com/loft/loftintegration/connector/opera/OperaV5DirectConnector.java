package com.loft.loftintegration.connector.opera;

import com.loft.loftintegration.config.PropertyProfile;
import com.loft.loftintegration.connector.PmsConnectionException;
import com.loft.loftintegration.connector.PmsConnector;
import com.loft.loftintegration.sync.model.FolioEvent;
import com.loft.loftintegration.sync.model.GuestProfileEvent;
import com.loft.loftintegration.sync.model.ReservationEvent;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct-access connector for on-prem Opera PMS v5, confirmed working
 * against The George Lagos's schema (Oracle 11g 11.2.0.3.0, ojdbc6
 * 11.2.0.4 required — see OperaConnectivityCheck for why).
 *
 * Schema notes from discovery (see OperaSchemaDiscovery):
 *  - RESERVATION_NAME is the actual reservation record, despite the name
 *    — RESV_NAME_ID is the key, NAME_ID links to the guest profile.
 *  - NAME is the guest profile table.
 *  - FINANCIAL_TRANSACTIONS is the folio/billing line-item table.
 *  - STAY_RECORDS carries room assignment (ROOM_NUMBER), joined in via
 *    PMS_RESV_NAME_ID (stored as text — TO_CHAR-cast to match).
 *  - NAME_PHONE carries both phone (PHONE_TYPE != 'EMAIL') and email
 *    (PHONE_TYPE = 'EMAIL', address in PHONE_NUMBER) — confirmed via
 *    OperaDistinctValues. NAME_ADDRESS does NOT hold email in this
 *    schema despite being the more obvious guess.
 *  - RESERVATION_NAME.RESV_STATUS holds plain-English values
 *    ('CHECKED OUT'/'CANCELLED'/'CHECKED IN'), confirmed via
 *    OperaDistinctValues — NOT the short codes from RESORT_BOOKING_STATUS
 *    (CXL/DEF/TEN/etc.), which turned out to be an unrelated table.
 *  - Guest profile pull filters out NAME_ID <= 0 — Opera's convention
 *    for system/pseudo profiles (e.g. NAME_ID=-9999 "Post It"), not
 *    real guests.
 *
 * Still unverified: whether the STAY_RECORDS join actually matches real
 * rows for every reservation (confirmed working for several, but not
 * exhaustively). Run OperaV5ConnectorTest to keep an eye on this.
 */
public class OperaV5DirectConnector implements PmsConnector {

    private Connection connection;
    private PropertyProfile property;

    // Each entity type gets its own checkpoint — sharing one mutable field
    // across all three pull methods was a real bug: pullReservationEvents()
    // advancing a shared checkpoint could push it past NAME's own last
    // update time before pullGuestProfileEvents() ever ran, silently
    // hiding real rows. All three start from the same Opera business date
    // in connect(), but advance independently from then on.
    private LocalDateTime reservationCheckpoint;
    private LocalDateTime guestProfileCheckpoint;
    private LocalDateTime folioCheckpoint;

    @Override
    public String connectorId() {
        return "opera-v5-direct";
    }

    @Override
    public void connect(PropertyProfile property) throws PmsConnectionException {
        this.property = property;
        var settings = property.getConnectionSettings();

        // Must happen before anything touches java.util.TimeZone — see
        // OperaConnectivityCheck for why (avoids ORA-12705 against this
        // Oracle 11g install, whose timezone region file predates
        // Africa/Lagos). This is process-wide, so ideally set once at
        // application startup rather than per-connect — fine here for v1.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

        String host = settings.get("host");
        String port = settings.get("port");
        String sid = settings.get("sid");
        String jdbcUrl = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;

        try {
            this.connection = DriverManager.getConnection(
                    jdbcUrl,
                    settings.get("username"),
                    settings.get("password")
            );
        } catch (SQLException e) {
            throw new PmsConnectionException(
                    "Failed to connect to Opera v5 DB for property " + property.getPropertyCode(), e);
        }

        // Anchor the checkpoint to Opera's own idea of "today", not the
        // JVM's real-world clock. Opera's RESORT table has no dedicated
        // business-date column in this schema version, but every row in
        // FINANCIAL_TRANSACTIONS carries one (BUSINESS_DATE), so its max
        // is a reliable stand-in for "what day Opera currently thinks it
        // is". This matters for two reasons: (1) it's correct even in a
        // deliberately backdated lab/training environment like this one,
        // and (2) it's arguably more correct even in real production,
        // since Opera's business date can lag the calendar date until
        // night audit runs.
        try {
            LocalDate businessDate = resolveOperaBusinessDate();
            // One day back from Opera's business date is a reasonable
            // starting window — wide enough to catch anything posted
            // "today" in Opera's terms, narrow enough not to re-pull
            // everything on every fresh connect. All three start here,
            // then advance independently as each is pulled.
            LocalDateTime initialCheckpoint = businessDate.atStartOfDay().minusDays(1);
            this.reservationCheckpoint = initialCheckpoint;
            this.guestProfileCheckpoint = initialCheckpoint;
            this.folioCheckpoint = initialCheckpoint;
        } catch (SQLException e) {
            throw new PmsConnectionException(
                    "Connected, but failed to resolve Opera's business date for property "
                            + property.getPropertyCode(), e);
        }
    }

    /**
     * Returns Opera's current business date, derived from
     * MAX(BUSINESS_DATE) in FINANCIAL_TRANSACTIONS.
     *
     * TODO: if a dedicated single-row status/config table with a real
     * "current business date" field turns up later (the RESORT table
     * doesn't have one in this schema version), switch to reading that
     * directly instead — it would be authoritative even on a business
     * day with zero transactions posted so far, which this MAX()
     * approach can't distinguish from "no data at all".
     */
    private LocalDate resolveOperaBusinessDate() throws SQLException {
        String sql = "SELECT MAX(BUSINESS_DATE) AS business_date FROM FINANCIAL_TRANSACTIONS";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                java.sql.Date date = rs.getDate("business_date");
                if (date != null) {
                    return date.toLocalDate();
                }
            }
        }
        throw new SQLException("FINANCIAL_TRANSACTIONS has no BUSINESS_DATE values to derive a business date from.");
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // best-effort close
            }
            connection = null;
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<ReservationEvent> pullReservationEvents() {
        // LEFT JOIN to STAY_RECORDS for room assignment — discovered via
        // OperaSchemaDiscovery. PMS_RESV_NAME_ID is stored as text there,
        // hence the TO_CHAR cast to match RESERVATION_NAME's numeric key.
        // TODO: verify this join actually matches real rows once tested —
        // the two tables were populated by different processes and the
        // text/number format alignment hasn't been confirmed end to end.
        String sql = """
                SELECT rn.RESV_NAME_ID, rn.RESORT, rn.NAME_ID, rn.BEGIN_DATE, rn.END_DATE,
                       rn.RESV_STATUS, rn.INSERT_DATE, rn.UPDATE_DATE,
                       sr.ROOM_NUMBER, sr.ROOM_LABEL
                FROM RESERVATION_NAME rn
                LEFT JOIN STAY_RECORDS sr ON sr.PMS_RESV_NAME_ID = TO_CHAR(rn.RESV_NAME_ID)
                WHERE rn.UPDATE_DATE > ?
                ORDER BY rn.UPDATE_DATE
                """;

        List<ReservationEvent> events = new ArrayList<>();
        Timestamp checkpointParam = Timestamp.valueOf(reservationCheckpoint);
        LocalDateTime newCheckpoint = reservationCheckpoint;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, checkpointParam);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ReservationEvent event = new ReservationEvent();
                    event.setReservationId(String.valueOf(rs.getLong("RESV_NAME_ID")));
                    event.setPropertyCode(rs.getString("RESORT"));
                    event.setGuestProfileId(String.valueOf(rs.getLong("NAME_ID")));
                    event.setArrivalDate(toLocalDate(rs.getDate("BEGIN_DATE")));
                    event.setDepartureDate(toLocalDate(rs.getDate("END_DATE")));

                    // Room number now comes from the STAY_RECORDS join above.
                    // roomType still unmapped — ROOM_LABEL looks like a
                    // display label (e.g. formatted room number), not an
                    // actual room type/category. TODO: join
                    // ROOM_CATEGORY_TEMPLATE or RATE_ROOM_CATEGORIES if a
                    // real room type description is needed later.
                    event.setRoomType(null);
                    event.setRoomNumber(rs.getString("ROOM_NUMBER"));

                    event.setChangeType(inferChangeType(
                            rs.getString("RESV_STATUS"),
                            rs.getTimestamp("INSERT_DATE"),
                            rs.getTimestamp("UPDATE_DATE")));

                    LocalDateTime eventTime = rs.getTimestamp("UPDATE_DATE").toLocalDateTime();
                    event.setEventTimestamp(eventTime);
                    events.add(event);

                    if (eventTime.isAfter(newCheckpoint)) {
                        newCheckpoint = eventTime;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed pulling reservation events for property "
                    + property.getPropertyCode(), e);
        }

        reservationCheckpoint = newCheckpoint;
        return events;
    }

    @Override
    public List<GuestProfileEvent> pullGuestProfileEvents() {
        // Confirmed via OperaDistinctValues: NAME_ADDRESS.ADDRESS_TYPE has
        // no 'EMAIL' value in this schema (real values: HOME, H, AR
        // ADDRESS, BUSINESS, PURGE) — email doesn't live there. It's
        // actually stored as a row in NAME_PHONE with PHONE_TYPE='EMAIL'
        // (confirmed: NAME_PHONE.PHONE_TYPE has HOME/BUSINESS/EMAIL/
        // MOBILE), with the address itself sitting in PHONE_NUMBER.
        // The phone lookup below explicitly excludes PHONE_TYPE='EMAIL'
        // so an email string can't land in the phone field.
        //
        // NAME_ID > 0 filters out Opera's system/pseudo profiles — seen
        // in real test data as NAME_ID=-9999 "Post It", used internally
        // for postings not tied to a real guest. Negative IDs are the
        // confirmed pattern from that observation; if a future property
        // turns up a different pseudo-profile convention, revisit this.
        String sql = """
                SELECT n.NAME_ID, n.FIRST, n.LAST, n.INSERT_DATE, n.UPDATE_DATE,
                       (SELECT MAX(PHONE_NUMBER) KEEP (DENSE_RANK FIRST ORDER BY PRIMARY_YN DESC)
                        FROM NAME_PHONE np
                        WHERE np.NAME_ID = n.NAME_ID AND np.PHONE_TYPE = 'EMAIL') AS EMAIL,
                       (SELECT MAX(PHONE_NUMBER) KEEP (DENSE_RANK FIRST ORDER BY PRIMARY_YN DESC)
                        FROM NAME_PHONE np
                        WHERE np.NAME_ID = n.NAME_ID
                        AND (np.PHONE_TYPE IS NULL OR np.PHONE_TYPE != 'EMAIL')) AS PHONE
                FROM NAME n
                WHERE n.UPDATE_DATE > ? AND n.NAME_ID > 0
                ORDER BY n.UPDATE_DATE
                """;

        List<GuestProfileEvent> events = new ArrayList<>();
        Timestamp checkpointParam = Timestamp.valueOf(guestProfileCheckpoint);
        LocalDateTime newCheckpoint = guestProfileCheckpoint;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, checkpointParam);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    GuestProfileEvent event = new GuestProfileEvent();
                    event.setGuestProfileId(String.valueOf(rs.getLong("NAME_ID")));
                    // NAME table has no per-row property/resort column in this
                    // schema — Opera profiles aren't partitioned per-property
                    // here, so we tag with this connector's configured property.
                    event.setPropertyCode(property.getPropertyCode());
                    event.setFirstName(rs.getString("FIRST"));
                    event.setLastName(rs.getString("LAST"));

                    // Email/phone both come from NAME_PHONE now, split by
                    // PHONE_TYPE — see class-level doc for why.
                    event.setEmail(rs.getString("EMAIL"));
                    event.setPhone(rs.getString("PHONE"));

                    Timestamp insertTs = rs.getTimestamp("INSERT_DATE");
                    Timestamp updateTs = rs.getTimestamp("UPDATE_DATE");
                    event.setChangeType(insertTs.equals(updateTs)
                            ? GuestProfileEvent.ChangeType.NEW
                            : GuestProfileEvent.ChangeType.MODIFIED);

                    LocalDateTime eventTime = updateTs.toLocalDateTime();
                    event.setEventTimestamp(eventTime);
                    events.add(event);

                    if (eventTime.isAfter(newCheckpoint)) {
                        newCheckpoint = eventTime;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed pulling guest profile events for property "
                    + property.getPropertyCode(), e);
        }

        guestProfileCheckpoint = newCheckpoint;
        return events;
    }

    @Override
    public List<FolioEvent> pullFolioEvents() {
        String sql = """
                SELECT TRX_NO, RESORT, RESV_NAME_ID, FOLIO_NO, TRX_AMOUNT,
                       CURRENCY, O_TRX_DESC, UPDATE_DATE
                FROM FINANCIAL_TRANSACTIONS
                WHERE UPDATE_DATE > ?
                ORDER BY UPDATE_DATE
                """;

        List<FolioEvent> events = new ArrayList<>();
        Timestamp checkpointParam = Timestamp.valueOf(folioCheckpoint);
        LocalDateTime newCheckpoint = folioCheckpoint;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, checkpointParam);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FolioEvent event = new FolioEvent();

                    long folioNo = rs.getLong("FOLIO_NO");
                    // FOLIO_NO can be null on some transaction rows — fall
                    // back to the transaction number so we still have a
                    // usable identifier rather than dropping the row.
                    event.setFolioId(rs.wasNull() ? "TRX-" + rs.getLong("TRX_NO") : String.valueOf(folioNo));

                    // RESV_NAME_ID can genuinely be null too (e.g. a walk-in
                    // POS charge not tied to any reservation) — getLong()
                    // silently returns 0 for null, which would misleadingly
                    // look like "reservation 0". Check wasNull() explicitly
                    // so an unlinked charge shows as null, not a fake ID.
                    long resvNameId = rs.getLong("RESV_NAME_ID");
                    event.setReservationId(rs.wasNull() ? null : String.valueOf(resvNameId));

                    event.setPropertyCode(rs.getString("RESORT"));

                    BigDecimal amount = rs.getBigDecimal("TRX_AMOUNT");
                    event.setAmount(amount);
                    event.setCurrency(rs.getString("CURRENCY"));
                    event.setDescription(rs.getString("O_TRX_DESC"));

                    // Simplification for v1: every row is a posted charge.
                    // TODO: refine once we've looked at IND_ADJUSTMENT_YN /
                    // REVERSE_PAYMENT_TRX_NO to distinguish real voids from
                    // normal charges.
                    event.setEventType(FolioEvent.EventType.CHARGE_POSTED);

                    LocalDateTime eventTime = rs.getTimestamp("UPDATE_DATE").toLocalDateTime();
                    event.setEventTimestamp(eventTime);
                    events.add(event);

                    if (eventTime.isAfter(newCheckpoint)) {
                        newCheckpoint = eventTime;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed pulling folio events for property "
                    + property.getPropertyCode(), e);
        }

        folioCheckpoint = newCheckpoint;
        return events;
    }

    private LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }

    /**
     * Confirmed via OperaDistinctValues against real data: RESERVATION_NAME
     * .RESV_STATUS holds its own plain-English values ('CHECKED OUT',
     * 'CANCELLED', 'CHECKED IN' — all 40 rows in the lab accounted for by
     * these three) — NOT the short codes from RESORT_BOOKING_STATUS
     * (CXL/DEF/TEN/etc.), which turned out to be an unrelated lookup
     * table. Exact match now that the real value is confirmed, rather
     * than the substring guess this replaced.
     */
    private ReservationEvent.ChangeType inferChangeType(String resvStatus, Timestamp insertTs, Timestamp updateTs) {
        if ("CANCELLED".equalsIgnoreCase(resvStatus)) {
            return ReservationEvent.ChangeType.CANCELLED;
        }
        if (insertTs != null && insertTs.equals(updateTs)) {
            return ReservationEvent.ChangeType.NEW;
        }
        return ReservationEvent.ChangeType.MODIFIED;
    }

    /**
     * Posts a charge to a guest's Opera folio via FINANCIAL_TRANSACTIONS.
     *
     * UNVERIFIED — this is a best-effort INSERT built from the columns
     * we discovered while reading this table, NOT from confirmed valid
     * values for a real insert. This is the first WRITE this connector
     * makes to Opera; unlike everything else in this class (all reads),
     * getting this wrong risks inserting a malformed transaction into a
     * real property's financial records. Do not trust this against a
     * real production Opera install without verifying the items below
     * against THAT install's actual configuration first:
     *
     *  - TRX_CODE: hardcoded below to "MISC" as a placeholder. Real
     *    Opera installs have a configured set of valid transaction
     *    codes (spa charges, room charges, F&B, etc.) — "MISC" may not
     *    exist or may not be appropriate. Find the real code via the
     *    Opera application's transaction code setup screen, or query
     *    whatever table holds it (not yet discovered — TRX_CODE
     *    appears in FINANCIAL_TRANSACTIONS itself but we haven't found
     *    the lookup/definition table for valid codes).
     *  - TRX_NO: NOT NULL in the schema, and we don't know whether
     *    Opera expects the client to supply this (e.g. from an Oracle
     *    SEQUENCE) or whether a trigger populates it automatically on
     *    insert. Attempting to let Oracle auto-generate it below by
     *    omitting it from the INSERT — if the real column is NOT NULL
     *    with no default/trigger, this INSERT will fail loudly (safer
     *    than silently succeeding with a wrong value).
     *  - FT_SUBTYPE, TC_GROUP, TC_SUBGROUP: all NOT NULL, values below
     *    are guesses ("CG" for "charge", generic group codes) based on
     *    common Opera conventions, NOT confirmed against this specific
     *    install's configuration.
     *
     * Gated behind tac-integration.opera-folio-posting-enabled in
     * application.yml (default false) for exactly this reason — see
     * BillingService, which checks that flag before ever calling this.
     */
    @Override
    public void postFolioCharge(String reservationId, BigDecimal amount, String currency, String description) throws SQLException {
        String sql = """
                INSERT INTO FINANCIAL_TRANSACTIONS
                    (RESORT, FT_SUBTYPE, TC_GROUP, TC_SUBGROUP, TRX_CODE, TRX_DATE, BUSINESS_DATE,
                     CURRENCY, RESV_NAME_ID, TRX_AMOUNT, POSTED_AMOUNT, O_TRX_DESC, DEFERRED_YN,
                     INSERT_USER, INSERT_DATE, UPDATE_USER, UPDATE_DATE)
                VALUES
                    (?, 'CG', 'MISC', 'MISC', 'MISC', SYSDATE, SYSDATE,
                     ?, ?, ?, ?, ?, 'N',
                     -1, SYSDATE, -1, SYSDATE)
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, property.getPropertyCode());
            stmt.setString(2, currency);
            stmt.setLong(3, Long.parseLong(reservationId));
            stmt.setBigDecimal(4, amount);
            stmt.setBigDecimal(5, amount);
            stmt.setString(6, description);
            stmt.executeUpdate();
        }
    }
}

