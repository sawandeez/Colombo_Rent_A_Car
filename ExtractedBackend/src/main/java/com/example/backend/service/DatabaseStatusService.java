package com.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DatabaseStatusService {

    private final MongoTemplate mongoTemplate;
    private volatile boolean isConnected = false;

    public DatabaseStatusService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        checkDatabaseConnection();
    }

    private void checkDatabaseConnection() {
        try {
            // Attempt to ping the MongoDB server
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            isConnected = true;
            log.info("✓ MongoDB connection established successfully");
        } catch (Exception e) {
            isConnected = false;
            log.error("✗ MongoDB connection failed: {}", e.getMessage());
        }
    }

    public String getDatabaseStatus() {
        try {
            // Attempt a ping to verify connection is still active
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            return "connected";
        } catch (Exception e) {
            log.warn("Database connection check failed: {}", e.getMessage());
            return "disconnected";
        }
    }

    public boolean isConnectedToDatabase() {
        return isConnected;
    }
}
