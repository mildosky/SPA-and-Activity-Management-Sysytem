package com.loft.loftintegration.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Standalone check — NOT part of the Spring Boot application.
 *
 * Answers one question directly: is the checkpoint-based filtering in
 * OperaV5DirectConnector finding zero rows because the data really is
 * older than the lookback window, or because something else is wrong?
 * Prints MIN/MAX(UPDATE_DATE) and row counts for the three tables the
 * connector queries, with no date filter at all.
 */
public class OperaDataFreshnessCheck {

    private static final String HOST = "192.168.56.3";
    private static final String PORT = "1521";
    private static final String SID = "OPERA";
    private static final String USERNAME = "opera";
    private static final String PASSWORD = "opera";

    public static void main(String[] args) throws Exception {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        String jdbcUrl = "jdbc:oracle:thin:@" + HOST + ":" + PORT + ":" + SID;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD)) {
            checkTable(conn, "RESERVATION_NAME");
            checkTable(conn, "NAME");
            checkTable(conn, "FINANCIAL_TRANSACTIONS");
        }
    }

    private static void checkTable(Connection conn, String table) throws Exception {
        String sql = "SELECT COUNT(*) AS cnt, MIN(UPDATE_DATE) AS min_upd, MAX(UPDATE_DATE) AS max_upd FROM " + table;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println(table + ":");
                System.out.println("  row count:      " + rs.getLong("cnt"));
                System.out.println("  earliest update: " + rs.getTimestamp("min_upd"));
                System.out.println("  latest update:   " + rs.getTimestamp("max_upd"));
                System.out.println();
            }
        }
    }
}
