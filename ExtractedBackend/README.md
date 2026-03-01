# Extracted Backend Project

This is a standalone Spring Boot backend project created from the main car rental system with MongoDB Atlas integration.

## Features

* Layered architecture with packages for `controller`, `service`, `repository`, `model`, and `config`.
* Spring Data MongoDB integration with MongoDB Atlas support.
* Environment variable-based configuration for secure credential handling.
* Health check endpoint at `/api/health`.
* Database status endpoint at `/api/db-status` with connection verification.
* Automatic startup logging confirming DB connection status.
* Graceful handling of connection failures.

## Requirements

* Java 21 or later
* Maven 3.6+ (or wrapper)
* MongoDB Atlas account (or local MongoDB instance)

## Configuration

### Setting MongoDB Connection String

The application reads the MongoDB connection string from the `MONGODB_URI` environment variable.

#### Option 1: Set Environment Variable (Recommended)

**Windows (PowerShell):**
```powershell
$env:MONGODB_URI = "mongodb+srv://username:password@cluster.mongodb.net/database-name?retryWrites=true&w=majority"
```

**Windows (Command Prompt):**
```cmd
set MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/database-name?retryWrites=true&w=majority
```

**Linux/macOS:**
```bash
export MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/database-name?retryWrites=true&w=majority"
```

#### Option 2: Modify application.yml (Development Only)

Edit `src/main/resources/application.yml` and replace the `uri` value directly:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb+srv://your-username:your-password@your-cluster.mongodb.net/your-db-name
```

> **Note:** Never commit credentials to version control. Always use environment variables in production.

## Running the Application

1. Navigate to the project root:
   ```bash
   cd ExtractedBackend
   ```

2. Set the `MONGODB_URI` environment variable (see Configuration section above).

3. Build and start the app:
   ```bash
   mvn clean spring-boot:run
   ```

4. Watch the console for startup confirmation:
   ```
   ════════════════════════════════════════════
     Extracted Backend Application Started
   ════════════════════════════════════════════
     Server running on: http://localhost:8080
     Health endpoint: GET /api/health
     DB Status endpoint: GET /api/db-status
   ════════════════════════════════════════════
   ```

## Testing the Endpoints

### Health Check
```bash
curl http://localhost:8080/api/health
# → {"status":"Backend Running"}
```

### Database Status
```bash
curl http://localhost:8080/api/db-status
# → {"database":"connected"}
```

If MongoDB is not connected, the response will show:
```json
{"database":"disconnected"}
```

## Connection Troubleshooting

- **Connection Timeout:** Verify your Atlas IP whitelist includes your current IP.
- **Authentication Error:** Check username and password in the connection string.
- **Network Issues:** Ensure your firewall allows outbound connections to MongoDB Atlas (port 27017).
- **Check logs:** The application logs connection status at startup with detailed error messages if connection fails.

## Project Structure

```
ExtractedBackend/
├── src/
│   ├── main/
│   │   ├── java/com/example/backend/
│   │   │   ├── config/        # MongoDB & Spring configuration
│   │   │   ├── controller/    # REST endpoints
│   │   │   ├── service/       # Business logic
│   │   │   ├── model/         # Entity models
│   │   │   └── repository/    # Data access layer
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/example/backend/
└── pom.xml
```

---

**Note:** This project mirrors the structure and configuration patterns of the main car-rental-system project for consistency and maintainability.

