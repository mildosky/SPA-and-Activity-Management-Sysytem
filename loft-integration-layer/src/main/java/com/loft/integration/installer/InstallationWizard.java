package com.loft.integration.installer;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

@Component
public class InstallationWizard {

    private static final String CONFIG_FILE = "application.yml";
    private static final String ENV_FILE = ".env";

    public boolean isInstalled() {
        // Check if config file exists and contains DB URL
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                return content.contains("jdbc:oracle:thin:") && !content.contains("YOUR_ORACLE_HOST");
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public void runWizard() {
        System.out.println("========================================");
        System.out.println("  LOFT INTEGRATION LAYER - SETUP WIZARD");
        System.out.println("========================================");
        System.out.println("Welcome! This wizard will configure your Oracle Database connection.");
        System.out.println("Please ensure your Oracle Database is running and accessible.\n");

        Scanner scanner = new Scanner(System.in);

        try {
            String host = prompt(scanner, "Oracle Host (e.g., localhost)", "localhost");
            String port = prompt(scanner, "Oracle Port (e.g., 1521)", "1521");
            String serviceName = prompt(scanner, "Oracle Service Name (e.g., ORCL)", "ORCL");
            String username = prompt(scanner, "Database Username", "loft_user");
            String password = promptSecret(scanner, "Database Password");

            System.out.println("\nTesting connection to Oracle Database...");
            String jdbcUrl = String.format("jdbc:oracle:thin:@%s:%s/%s", host, port, serviceName);

            if (testConnection(jdbcUrl, username, password)) {
                System.out.println("✓ Connection successful!");
                saveConfiguration(host, port, serviceName, username, password);
                System.out.println("✓ Configuration saved to " + CONFIG_FILE);
                System.out.println("\nSetup complete! The application will now start with the new configuration.");
            } else {
                System.out.println("✗ Connection failed. Please check your credentials and try running the installer again.");
            }

        } catch (Exception e) {
            System.err.println("Error during setup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private String prompt(Scanner scanner, String message, String defaultValue) {
        System.out.print(message + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    private String promptSecret(Scanner scanner, String message) {
        System.out.print(message + ": ");
        // Simple masking (note: Console.readPassword is better but requires System.console())
        return scanner.nextLine().trim();
    }

    private boolean testConnection(String url, String user, String pass) {
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    private void saveConfiguration(String host, String port, String serviceName, String username, String password) throws IOException {
        String yamlContent = String.format(
                "spring:\n" +
                "  datasource:\n" +
                "    url: jdbc:oracle:thin:@%s:%s/%s\n" +
                "    username: %s\n" +
                "    password: %s\n" +
                "    driver-class-name: oracle.jdbc.OracleDriver\n" +
                "  jpa:\n" +
                "    hibernate:\n" +
                "      ddl-auto: validate\n" +
                "    properties:\n" +
                "      hibernate:\n" +
                "        dialect: org.hibernate.dialect.OracleDialect\n" +
                "        format_sql: true\n" +
                "  flyway:\n" +
                "    enabled: true\n" +
                "    locations: classpath:db/migration\n" +
                "    baseline-on-migrate: true\n" +
                "\n" +
                "server:\n" +
                "  port: 8080\n" +
                "\n" +
                "loft:\n" +
                "  integration:\n" +
                "    opera:\n" +
                "      enabled: false # Enable after configuring Opera OXI details\n",
                host, port, serviceName, username, password
        );

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(yamlContent);
        }
        
        // Also save to .env for Docker/Cloud compatibility
        String envContent = String.format(
                "LOFT_DB_URL=jdbc:oracle:thin:@%s:%s/%s\n" +
                "LOFT_DB_USERNAME=%s\n" +
                "LOFT_DB_PASSWORD=%s\n",
                host, port, serviceName, username, password
        );
        
        try (FileWriter writer = new FileWriter(ENV_FILE)) {
            writer.write(envContent);
        }
    }
}
