package com.example.backend.controller;

import com.example.backend.service.HealthService;
import com.example.backend.service.DatabaseStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;
    private final DatabaseStatusService databaseStatusService;

    public HealthController(HealthService healthService, DatabaseStatusService databaseStatusService) {
        this.healthService = healthService;
        this.databaseStatusService = databaseStatusService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", healthService.getStatus());
    }

    @GetMapping("/db-status")
    public Map<String, String> dbStatus() {
        String status = databaseStatusService.getDatabaseStatus();
        return Map.of("database", status);
    }
}
