package com.example.backend.controller;

import com.example.backend.service.HealthService;
import com.example.backend.service.DatabaseStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
public class HealthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private HealthService healthService;

    @MockBean
    private DatabaseStatusService databaseStatusService;

    @BeforeEach
    void setup() {
        when(healthService.getStatus()).thenReturn("Backend Running");
        when(databaseStatusService.getDatabaseStatus()).thenReturn("connected");
    }

    @Test
    void healthEndpointReturnsStatus() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"Backend Running\"}"));
    }

    @Test
    void dbStatusEndpointReturnsConnected() throws Exception {
        mvc.perform(get("/api/db-status"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"database\":\"connected\"}"));
    }

    @Test
    void dbStatusEndpointHandlesDisconnection() throws Exception {
        when(databaseStatusService.getDatabaseStatus()).thenReturn("disconnected");

        mvc.perform(get("/api/db-status"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"database\":\"disconnected\"}"));
    }
}
