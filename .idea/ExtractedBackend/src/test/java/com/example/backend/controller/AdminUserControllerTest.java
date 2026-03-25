package com.example.backend.controller;

import com.example.backend.dto.AdminUserResponse;
import com.example.backend.security.JwtService;
import com.example.backend.security.SecurityConfiguration;
import com.example.backend.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@Import(SecurityConfiguration.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanFetchExistingUserById() throws Exception {
        AdminUserResponse userResponse = AdminUserResponse.builder()
                .id("user-123")
                .name("John Doe")
                .email("john@example.com")
                .username("john@example.com")
                .build();

        when(adminUserService.getUserById("user-123")).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/admin/users/user-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.username").value("john@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminGetUserReturns404ForUnknownUserId() throws Exception {
        when(adminUserService.getUserById("unknown-id"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/v1/admin/users/unknown-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void nonAdminGetsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/user-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGetUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/user-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

