package com.loft.loftintegration.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Standalone connectivity check — NOT part of the Spring Boot application.
 *
 * Run this directly (right-click > Run in IntelliJ, or `mvn exec:java`)
 * before touching OperaV5DirectConnector. It does one thing only: opens a
 * JDBC connection to the Opera v5 Oracle DB and runs `SELECT 1 FROM DUAL`.
 * If this fails, the problem is network/credentials/driver — not your
 * connector code, so there's no point debugging Java logic yet.
 *
 * Usage (edit the constants below, or pass as program arguments in this
 * order: host port sid username password):
 *
 *   java -cp <classpath> com.loft.loftintegration.tools.OperaConnectivityCheck \
 *        192.168.56.3 1521 OPERA opera opera
 */
public class OperaConnectivityCheck {

    public static void main(String[] args) {
        // Must happen before ANYTHING else touches java.util.TimeZone —
        // once the JVM's default timezone is read once, it's cached, and
        // setting the property later has no effect. Oracle 11g's timezone
        // region file often doesn't include newer/smaller regions like
        // Africa/Lagos, which the driver reports during login — that
        // mismatch is what ORA-12705 actually is here. Forcing UTC sends
        // a region Oracle 11g always recognizes.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

        String host = args.length > 0 ? args[0] : "192.168.56.3";
        String port = args.length > 1 ? args[1] : "1521";
        String sid = args.length > 2 ? args[2] : "OPERA";
        String username = args.length > 3 ? args[3] : "opera";
        String password = args.length > 4 ? args[4] : "opera";

        String jdbcUrl = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;

        // Oracle 11g (this Opera install) doesn't recognize the timezone
        // REGION names newer JDBC drivers (23.x) try to negotiate during
        // login, which surfaces as ORA-12705 "Cannot access NLS data files"
        // even with correct host/port/SID/credentials. Forcing an absolute
        // offset instead of a region name avoids the mismatch.
        System.setProperty("oracle.jdbc.timezoneAsRegion", "false");

        System.out.println("Connecting to " + jdbcUrl + " as " + username + " ...");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL")) {

            if (rs.next()) {
                System.out.println("SUCCESS: connected and query returned " + rs.getInt(1));
                System.out.println("JDBC driver: " + conn.getMetaData().getDriverVersion());
                System.out.println("DB product:  " + conn.getMetaData().getDatabaseProductVersion());
            } else {
                System.out.println("Connected, but query returned no rows (unexpected for SELECT 1 FROM DUAL).");
            }

        } catch (Exception e) {
            System.out.println("FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            System.out.println();
            System.out.println("Common causes:");
            System.out.println("  - Wrong host/port/SID -> check tnsnames.ora on the Opera server again");
            System.out.println("  - Network unreachable  -> confirm your dev machine is on the same Tailscale network");
            System.out.println("  - Auth failure          -> confirm username/password are correct for this schema");
            System.out.println("  - Listener not accepting external connections -> check listener.ora on the server");
            e.printStackTrace();
        }
    }
}
