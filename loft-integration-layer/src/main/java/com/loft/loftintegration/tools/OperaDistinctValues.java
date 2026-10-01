package com.loft.loftintegration.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Standalone diagnostic — NOT part of the Spring Boot application.
 *
 * Prints distinct values (with counts) for one column in one table.
 * Use this whenever a query is guessing at a code value (like
 * ADDRESS_TYPE = 'EMAIL') instead of assuming it's right — cheaper than
 * finding out via silent empty results.
 *
 * Usage: two program arguments, table name then column name.
 *   OperaDistinctValues NAME_ADDRESS ADDRESS_TYPE
 *   OperaDistinctValues NAME_PHONE PHONE_TYPE
 */
public class OperaDistinctValues {

    private static final String HOST = "192.168.56.3";
    private static final String PORT = "1521";
    private static final String SID = "OPERA";
    private static final String USERNAME = "opera";
    private static final String PASSWORD = "opera";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: OperaDistinctValues <table> <column>");
            return;
        }
        String table = args[0].toUpperCase();
        String column = args[1].toUpperCase();

        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        String jdbcUrl = "jdbc:oracle:thin:@" + HOST + ":" + PORT + ":" + SID;

        String sql = "SELECT " + column + ", COUNT(*) AS cnt FROM " + table
                + " GROUP BY " + column + " ORDER BY cnt DESC";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("Distinct values of " + table + "." + column + ":");
            System.out.println();
            System.out.printf("%-30s %s%n", "VALUE", "COUNT");
            System.out.println("-".repeat(40));

            while (rs.next()) {
                String value = rs.getString(1);
                long count = rs.getLong("cnt");
                System.out.printf("%-30s %d%n", value == null ? "(null)" : "'" + value + "'", count);
            }
        }
    }
}
