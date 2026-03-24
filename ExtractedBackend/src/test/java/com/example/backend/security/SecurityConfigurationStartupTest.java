package com.example.backend.security;

import com.example.backend.controller.VehicleController;
import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.service.VehicleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VehicleController.class)
@Import(SecurityConfiguration.class)
class SecurityConfigurationStartupTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    /**
     * The Mockito mock of JwtAuthenticationFilter stubs doFilterInternal() as a no-op,
     * which means filterChain.doFilter() is never called and the request never reaches
     * the DispatcherServlet.  Stub it here to properly pass through the chain.
     *
     * doFilterInternal is protected but accessible from this package
     * (com.example.backend.security).
     */
    @BeforeEach
    void letJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest  req   = invocation.getArgument(0);
            HttpServletResponse res   = invocation.getArgument(1);
            FilterChain         chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter)
          .doFilterInternal(
              any(HttpServletRequest.class),
              any(HttpServletResponse.class),
              any(FilterChain.class));
    }

    @Test
    void permitAllVehicleListEndpointLoadsSecurityFilterChainAndResponds() throws Exception {
        when(vehicleService.getAllVehicles()).thenReturn(List.of(new VehicleSummaryDto("Car1", "thumb1")));

        mockMvc.perform(get("/api/v1/vehicles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Car1"));
    }
}
