# MongoDB Configuration for ExtractedBackend

This guide explains how to set up MongoDB Atlas and connect the ExtractedBackend project.

## Quick Start (Windows)

### Step 1: Get MongoDB Atlas Connection String

1. Go to [MongoDB Atlas Dashboard](https://cloud.mongodb.com)
2. Click your cluster → **Connect**
3. Select **Drivers** → Choose **Java**
4. Copy the connection string (looks like): 
   ```
   mongodb+srv://username:password@cluster.mongodb.net/databasename?retryWrites=true&w=majority
   ```

### Step 2: Set Environment Variable (PowerShell)

```powershell
$env:MONGODB_URI = "mongodb+srv://username:password@cluster.mongodb.net/databasename?retryWrites=true&w=majority"
```

### Step 3: Run the Application

```bash
cd ExtractedBackend
mvn clean spring-boot:run
```

### Step 4: Verify Connection

In another terminal, test the endpoints:

```bash
# Health check
curl http://localhost:8080/api/health
# Response: {"status":"Backend Running"}

# Database status
curl http://localhost:8080/api/db-status
# Response: {"database":"connected"}
```

---

## Detailed Setup Instructions

### Windows Command Prompt (Alternative)

```cmd
set MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/databasename?retryWrites=true&w=majority
mvn clean spring-boot:run
```

### Linux/macOS

```bash
export MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/databasename?retryWrites=true&w=majority"
cd ExtractedBackend
mvn clean spring-boot:run
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **Connection Timeout** | Check if your IP is whitelisted in MongoDB Atlas security settings |
| **Authentication Failed** | Verify username/password in the connection string are correct |
| **Unknown Database** | Create the database in MongoDB Atlas or check the name in the URI |
| **Port Already in Use** | Change port in `application.yml` (default: 8080) |
| **Database shows disconnected** | Check the error logs in console; verify network connectivity |

---

## Expected Startup Log

```
════════════════════════════════════════════
  Extracted Backend Application Started
════════════════════════════════════════════
  Server running on: http://localhost:8080
  Health endpoint: GET /api/health
  DB Status endpoint: GET /api/db-status
════════════════════════════════════════════
✓ MongoDB connection established successfully
```

---

## Project Architecture

The extended project follows the same layered architecture as the main project:

- **config/** → MongoDB & Spring configuration (MongoConfig.java)
- **controller/** → REST endpoints (HealthController.java)
- **service/** → Business logic (HealthService.java, DatabaseStatusService.java)
- **model/** → Entity classes
- **repository/** → Data access layer (placeholders for future)

---

## What's Inside

✅ Spring Boot 3.1.4  
✅ Spring Data MongoDB  
✅ Java 21  
✅ Lombok for cleaner code  
✅ Health & DB status endpoints  
✅ Environment variable-based configuration  
✅ Graceful connection error handling  
✅ Comprehensive logging  
✅ Unit tests included  

---

## Next Steps

Once the database is connected, you can:

1. Create MongoDB document models in the `model/` package
2. Create repository interfaces extending `MongoRepository`
3. Implement service business logic
4. Add new REST endpoints to the controller

The foundation is ready for rapid feature development! 🚀
