package com.example.backend.controller;

import com.example.backend.service.HealthService;
import com.example.backend.service.DatabaseStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HealthControllerTest {

    private HealthService healthService;
    private DatabaseStatusService databaseStatusService;
    private HealthController controller;

    @BeforeEach
    void setup() {
        healthService = new HealthService();
        // override to return running
        healthService = new HealthService() {
            @Override
            public String getStatus() {
                return "Backend Running";
            }
        };
        databaseStatusService = new DatabaseStatusService(null) {
            @Override
            public String getDatabaseStatus() {
                return "connected";
            }
        };
        controller = new HealthController(healthService, databaseStatusService);
    }

    @Test
    void healthReturnsRunning() {
        var response = controller.health();
        assertEquals("Backend Running", response.get("status"));
    }

    @Test
    void dbStatusReturnsConnected() {
        var response = controller.dbStatus();
        assertEquals("connected", response.get("database"));
    }

    @Test
    void dbStatusHandlesDisconnection() {
        // simulate service failure by overriding
        databaseStatusService = new DatabaseStatusService(null) {
            @Override
            public String getDatabaseStatus() {
                throw new RuntimeException("fail");
            }
        };
        controller = new HealthController(healthService, databaseStatusService);
        try {
            controller.dbStatus();
            fail("Expected exception");
        } catch (Exception e) {
            // expected
        }
    }
}

