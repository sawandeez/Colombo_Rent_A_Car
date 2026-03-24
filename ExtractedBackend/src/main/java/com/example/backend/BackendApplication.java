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

        String port = context.getEnvironment().getProperty("local.server.port");
        if (port == null || port.isBlank()) {
            port = context.getEnvironment().getProperty("server.port", "8080");
        }
        
        log.info("════════════════════════════════════════════");
        log.info("  Extracted Backend Application Started");
        log.info("════════════════════════════════════════════");
        log.info("  Server running on: http://localhost:{}", port);
        log.info("  Health endpoint: GET /api/health");
        log.info("  DB Status endpoint: GET /api/db-status");
        log.info("════════════════════════════════════════════");
    }
}
