package com.example.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BackendApplication.class, args);
        
        log.info("════════════════════════════════════════════");
        log.info("  Extracted Backend Application Started");
        log.info("════════════════════════════════════════════");
        log.info("  Server running on: http://localhost:8080");
        log.info("  Health endpoint: GET /api/health");
        log.info("  DB Status endpoint: GET /api/db-status");
        log.info("════════════════════════════════════════════");
    }
}
