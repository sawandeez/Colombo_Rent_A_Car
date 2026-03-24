# Extracted Backend Project

This is a standalone Spring Boot backend project created from the main car rental system with MongoDB Atlas integration, data seeding, and vehicle management APIs.

## Features

* **Layered architecture** with packages for `controller`, `service`, `repository`, `model`, and `config`.
* **Spring Data MongoDB** integration with MongoDB Atlas support.
* **Environment variable-based configuration** for secure credential handling.
* **MongoDB Document Models**:
  - `VehicleType` – Vehicle categories (SUV, Sedan, Van)
  - `Vehicle` – Individual vehicle records with pricing and availability
* **Automatic Data Seeding**:
  - Seeds 3 vehicle types on startup if none exist
  - Seeds 3 linked vehicles with type references
  - Idempotent design – only seeds if database is empty
* **REST Endpoints** for vehicle management and system health
* **Graceful connection error handling** with detailed logging
* **Unit test coverage** for core functionality

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
      uri: mongodb+srv://your-username:your-password@your-cluster.mongodb.net/your-db-name?retryWrites=true&w=majority
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
     ✓ MongoDB connection established successfully
     Seeded vehicle types: SUV, Sedan, Van
     Seeded 3 vehicles linked to types
   ```

## API Endpoints

### Health & Status
```bash
# Application health check
GET /api/health
# Response: {"status":"Backend Running"}

# Database connection status
GET /api/db-status
# Response: {"database":"connected"}

# Seeding verification
GET /api/seed-status
# Response: {"vehicleTypes":3,"vehicles":3,"message":"Database seeding complete"}
```

### Vehicle Management
```bash
# Get all vehicles
GET /api/vehicles
# Response: [
#   {
#     "id": "...",
#     "name": "Toyota RAV4",
#     "thumbnailUrl": "https://example.com/rav4.jpg",
#     "vehicleTypeId": "...",
#     "rentalPrice": 8000,
#     "availabilityStatus": "AVAILABLE"
#   },
#   ...
# ]

# Get all vehicle types
GET /api/vehicle-types
# Response: [
#   {"id": "...", "name": "SUV"},
#   {"id": "...", "name": "Sedan"},
#   {"id": "...", "name": "Van"}
# ]
```

## Data Seeding Details

On first startup, the `DataSeeder` class automatically populates the database with:

### Vehicle Types
- **SUV** – Sport Utility Vehicles
- **Sedan** – Four-door passenger cars
- **Van** – Multi-passenger transport vehicles

### Vehicles
1. **Toyota RAV4** (SUV)
   - Rental Price: ₹8,000/day
   - Status: AVAILABLE

2. **Honda Civic** (Sedan)
   - Rental Price: ₹5,000/day
   - Status: AVAILABLE

3. **Ford Transit** (Van)
   - Rental Price: ₹9,000/day
   - Status: UNAVAILABLE

The seeding is idempotent – it only runs if the respective collections are empty, making it safe to restart the application multiple times.

## Project Structure

```
ExtractedBackend/
├── src/
│   ├── main/
│   │   ├── java/com/example/backend/
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.java       # MongoDB & Spring configuration
│   │   │   │   └── DataSeeder.java      # Automatic data seeding
│   │   │   ├── controller/
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── VehicleController.java
│   │   │   │   └── SeedStatusController.java
│   │   │   ├── service/
│   │   │   │   ├── HealthService.java
│   │   │   │   └── DatabaseStatusService.java
│   │   │   ├── model/
│   │   │   │   ├── VehicleType.java     # MongoDB document
│   │   │   │   ├── Vehicle.java         # MongoDB document
│   │   │   │   └── HealthStatus.java
│   │   │   └── repository/
│   │   │       ├── VehicleTypeRepository.java
│   │   │       └── VehicleRepository.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/example/backend/
└── pom.xml
```

## Connection Troubleshooting

| Issue | Solution |
|-------|----------|
| **Connection Timeout** | Verify your Atlas IP whitelist includes your current IP. |
| **Authentication Error** | Check username and password in the connection string. |
| **Unknown Database** | Create the database in MongoDB Atlas or check the name in the URI. |
| **Port Already in Use** | Change port in `application.yml` (default: 8080). |
| **Database shows disconnected** | Check error logs in console; verify network connectivity. |

## Technology Stack

- **Backend:** Spring Boot 3.1.4, Java 21
- **Database:** MongoDB (Spring Data MongoDB)
- **Build:** Maven
- **Logging:** SLF4J with Lombok `@Slf4j`

## Next Steps

Once the database is connected, you can:

1. Create DTOs for API requests/responses
2. Implement service layer business logic
3. Add more REST endpoints for booking management
4. Integrate with authentication/authorization
5. Add pagination and filtering to list endpoints
6. Create custom queries in repositories if needed

---

**Note:** This project mirrors the structure and patterns of the main car-rental-system project for consistency and maintainability.


