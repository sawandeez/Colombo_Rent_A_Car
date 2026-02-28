# Extracted Backend Project

This is a standalone Spring Boot backend project created from the main car rental system.

## Features

* Layered architecture with packages for `controller`, `service`, `repository`, `model`, and `config`.
* A simple health check endpoint at `/api/health`.
* No database integration yet; repository package contains placeholders.

## Requirements

* Java 17 or later
* Maven 3.6+ (or wrapper)

## Running the application

1. Navigate to the project root:
   ```bash
   cd ExtractedBackend
   ```
2. Build and start the app:
   ```bash
   mvn spring-boot:run
   ```
3. Hit the health endpoint:
   ```bash
   curl http://localhost:8080/api/health
   # {"status":"Backend Running"}
   ```

The application starts successfully on port 8080 and returns the expected JSON response.
