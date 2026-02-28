package com.example.backend.controller;

import com.example.backend.service.HealthService;
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

    @BeforeEach
    void setup() {
        when(healthService.getStatus()).thenReturn("Backend Running");
    }

    @Test
    void healthEndpointReturnsStatus() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"Backend Running\"}"));
    }
}
