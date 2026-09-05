package com.loft.loftintegration.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * Standalone diagnostic — NOT part of the Spring Boot application.
 *
 * Dumps every row and column of a table, generically (works for any
 * column types via ResultSetMetaData). Useful for small lookup/reference
 * tables (status codes, reason codes, etc.) where OperaDistinctValues'
 * one-column-at-a-time view doesn't show how columns relate to each
 * other in the same row.
 *
 * Usage: table name, optional row limit (default 50 — deliberately NOT
 * for large transactional tables).
 *   OperaTableDump RESORT_BOOKING_STATUS
 *   OperaTableDump RESORT_BOOKING_STATUS 20
 */
public class OperaTableDump {

    private static final String HOST = "192.168.56.3";
    private static final String PORT = "1521";
    private static final String SID = "OPERA";
    private static final String USERNAME = "opera";
    private static final String PASSWORD = "opera";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: OperaTableDump <table> [rowLimit]");
            return;
        }
        String table = args[0].toUpperCase();
        int rowLimit = args.length > 1 ? Integer.parseInt(args[1]) : 50;

        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        String jdbcUrl = "jdbc:oracle:thin:@" + HOST + ":" + PORT + ":" + SID;

        String sql = "SELECT * FROM " + table + " WHERE ROWNUM <= " + rowLimit;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            System.out.println("Dumping up to " + rowLimit + " rows of " + table + ":");
            System.out.println();

            int rowNum = 0;
            while (rs.next()) {
                rowNum++;
                System.out.println("--- Row " + rowNum + " ---");
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = meta.getColumnName(i);
                    String value = rs.getString(i);
                    System.out.printf("  %-30s %s%n", columnName, value == null ? "(null)" : value);
                }
                System.out.println();
            }

            if (rowNum == 0) {
                System.out.println("(no rows)");
            }
        }
    }
}
