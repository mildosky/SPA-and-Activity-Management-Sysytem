package com.loft.loftintegration.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Standalone schema discovery tool — NOT part of the Spring Boot application.
 *
 * Run this to figure out what tables/columns actually exist in this Opera
 * v5 install, before writing real queries in OperaV5DirectConnector.
 * Opera schemas typically have several hundred tables, so this filters
 * to ones whose names suggest they're relevant to reservations, guest
 * profiles, and folios/billing — the three things the connector needs.
 *
 * Two modes:
 *
 *  1. No table name argument -> lists tables matching the keyword filter,
 *     with row counts, so you can see which ones actually have data.
 *
 *  2. A table name argument -> dumps that table's full column list
 *     (name, type, nullable) so you can see its exact structure.
 *
 * Usage (edit constants below, or pass as program arguments):
 *   No args:            lists candidate tables
 *   One arg (table name): dumps columns for that table
 *
 *   java -cp <classpath> com.loft.loftintegration.tools.OperaSchemaDiscovery
 *   java -cp <classpath> com.loft.loftintegration.tools.OperaSchemaDiscovery RESERVATION
 */
public class OperaSchemaDiscovery {

    // Same connection details as OperaConnectivityCheck — adjust if yours differ.
    private static final String HOST = "192.168.56.3";
    private static final String PORT = "1521";
    private static final String SID = "OPERA";
    private static final String USERNAME = "opera";
    private static final String PASSWORD = "opera";

    // Keywords used to filter the (likely hundreds of) tables down to ones
    // worth looking at first. Widen this list as you learn real table names.
    private static final List<String> KEYWORDS = Arrays.asList(
            "RESV", "RESERVATION", "PROFILE", "GUEST", "FOLIO", "TRANSACTION",
            "TXN", "ARRIVAL", "DEPARTURE", "ROOM", "STAY", "BOOKING",
            "ADDRESS", "PHONE", "EMAIL", "COMM", "ASSIGN", "OCCUP", "ALLOC"
    );

    public static void main(String[] args) throws Exception {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

        String jdbcUrl = "jdbc:oracle:thin:@" + HOST + ":" + PORT + ":" + SID;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD)) {

            if (args.length > 0) {
                dumpColumns(conn, args[0].toUpperCase());
            } else {
                listCandidateTables(conn);
            }

        } catch (Exception e) {
            System.out.println("FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lists tables owned by the connected user whose name contains any of
     * KEYWORDS, along with a row count for each — tables with 0 rows are
     * usually not worth building a connector query against.
     */
    private static void listCandidateTables(Connection conn) throws Exception {
        System.out.println("Scanning tables owned by " + USERNAME + " for keyword matches: " + KEYWORDS);
        System.out.println();

        List<String> matchingTables = new java.util.ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT table_name FROM user_tables ORDER BY table_name")) {

            while (rs.next()) {
                String tableName = rs.getString("table_name");
                for (String keyword : KEYWORDS) {
                    if (tableName.contains(keyword)) {
                        matchingTables.add(tableName);
                        break;
                    }
                }
            }
        }

        System.out.println("Found " + matchingTables.size() + " matching tables. Getting row counts...");
        System.out.println();
        System.out.printf("%-40s %s%n", "TABLE_NAME", "ROW_COUNT");
        System.out.println("-".repeat(55));

        try (Statement stmt = conn.createStatement()) {
            for (String tableName : matchingTables) {
                try (ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                    countRs.next();
                    long count = countRs.getLong(1);
                    System.out.printf("%-40s %d%n", tableName, count);
                } catch (Exception e) {
                    System.out.printf("%-40s (error: %s)%n", tableName, e.getMessage());
                }
            }
        }

        System.out.println();
        System.out.println("Run again with a table name argument to see its columns, e.g.:");
        System.out.println("  OperaSchemaDiscovery RESV_NAME_INDEX");
    }

    /** Dumps column name, data type, and nullability for one table. */
    private static void dumpColumns(Connection conn, String tableName) throws Exception {
        System.out.println("Columns for " + tableName + ":");
        System.out.println();
        System.out.printf("%-30s %-20s %s%n", "COLUMN_NAME", "DATA_TYPE", "NULLABLE");
        System.out.println("-".repeat(60));

        String sql = "SELECT column_name, data_type, data_length, nullable " +
                     "FROM user_tab_columns WHERE table_name = ? ORDER BY column_id";

        try (var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String col = rs.getString("column_name");
                    String type = rs.getString("data_type");
                    int length = rs.getInt("data_length");
                    String nullable = rs.getString("nullable");
                    System.out.printf("%-30s %-20s %s%n", col, type + "(" + length + ")", nullable);
                }
                if (!any) {
                    System.out.println("No columns found — check the table name is correct "
                            + "(case-sensitive match against user_tables) and that it's owned by "
                            + USERNAME + ".");
                }
            }
        }
    }
}
