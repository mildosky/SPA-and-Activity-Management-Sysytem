# Loft Integration Layer - Installation Guide

## Quick Start Installation

The Loft Integration Layer now includes an interactive installation wizard that configures everything during first-time setup on your property server. No external database hosting required - it uses Oracle Database (same as Opera PMS) which can run locally on your server.

### Automated Installation (Recommended)

Simply run the installer script on your server:

```bash
./install.sh
```

The installer will:
1. Check for Java 17+ installation
2. Build the application using Maven
3. Launch the Setup Wizard to configure your Oracle Database connection
4. Start the application automatically

### Manual Installation

If you prefer manual setup:

1. **Build the application:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Run the application:**
   ```bash
   java -jar target/loft-integration-layer-0.1.0-SNAPSHOT.jar
   ```

3. **Follow the Setup Wizard prompts** to configure your database connection.

## Setup Wizard Prompts

During first-time startup, you'll be prompted for:

- **Oracle Host**: Your Oracle Database server hostname (e.g., `localhost` or IP address)
- **Oracle Port**: Database port (default: `1521`)
- **Service Name**: Oracle service name (e.g., `ORCL`, `XE`, `pdborcl`)
- **Database Username**: Username for the Loft database user
- **Database Password**: Password for the database user

The wizard will:
- Test the connection to ensure credentials are correct
- Save configuration to `application.yml`
- Create a `.env` file for environment variable support
- Automatically start the application with the new configuration

## Prerequisites

Before running the installer, ensure:

1. **Java 17 or higher** is installed on the server
2. **Oracle Database** is installed and running (can be local or network-accessible)
3. **Maven** is installed (or use the included Maven wrapper)

## Oracle Database Setup

If you haven't created a database user yet, connect to Oracle as SYSDBA and run:

```sql
CREATE USER loft_user IDENTIFIED BY your_secure_password;
GRANT CONNECT, RESOURCE, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO loft_user;
ALTER USER loft_user QUOTA UNLIMITED ON USERS;
```

## Post-Installation

After successful installation:

1. The application will run Flyway migrations to create the schema
2. Access the guest booking UI at: `http://localhost:8080`
3. Access the staff admin dashboard at: `http://localhost:8080/admin`
4. Access the kiosk mode at: `http://localhost:8080/kiosk`

## Configuration Files

The installer creates/updates:

- `application.yml` - Main Spring Boot configuration
- `.env` - Environment variables for Docker/cloud deployment

## Running in Production

For production deployments:

1. Set environment variables instead of using the wizard:
   ```bash
   export LOFT_DB_URL=jdbc:oracle:thin:@db-server:1521/ORCL
   export LOFT_DB_USERNAME=loft_user
   export LOFT_DB_PASSWORD=secure_password
   ```

2. Run the application:
   ```bash
   java -jar loft-integration-layer-0.1.0-SNAPSHOT.jar
   ```

## Troubleshooting

**Connection Failed?**
- Verify Oracle Database is running
- Check firewall rules allow connections on port 1521
- Ensure the service name is correct (use `SELECT name FROM v$database;` in SQL*Plus)

**Build Failed?**
- Ensure Java 17+ is installed: `java -version`
- Ensure Maven is available or use `./mvnw`

**Already Installed?**
- To re-run the wizard, delete `application.yml` and restart the application
- Or manually edit `application.yml` with your database details

## Support

For assistance, contact your system administrator or refer to the full documentation in the project repository.
