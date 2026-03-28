package com.example.backend.controller;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.ProfileResponse;
import com.example.backend.dto.ProfileUpdateRequest;
import com.example.backend.security.JwtService;
import com.example.backend.security.SecurityConfiguration;
import com.example.backend.service.AuthService;
import com.example.backend.service.ProfileService;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfiguration.class)
class AuthControllerProfileWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void registerAcceptsFrontendCompatibilityPayload() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id("user-1")
                .token("jwt-token")
                .accessToken("jwt-token")
                .email("customer@example.com")
                .name("Customer One")
                .phone("+94771234567")
                .district("Colombo")
                .city("Nugegoda")
                .role("CUSTOMER")
                .documentsVerified(false)
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Customer One",
                                  "email": "customer@example.com",
                                  "password": "Password@123",
                                  "phone": "+94771234567",
                                  "age": 28,
                                  "district": "Colombo",
                                  "city": "No. 10 Main Street, Nugegoda",
                                  "address": "No. 10 Main Street"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @WithMockUser(username = "current@example.com", roles = "CUSTOMER")
    void updateProfileSucceedsForAuthenticatedUser() throws Exception {
        ProfileResponse response = ProfileResponse.builder()
                .id("user-1")
                .name("Updated Name")
                .email("updated@example.com")
                .phone("0771234567")
                .district("Colombo")
                .city("Maharagama")
                .role("CUSTOMER")
                .documentsVerified(false)
                .build();

        when(profileService.updateCurrentUserProfile(any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Name",
                                  "email": "updated@example.com",
                                  "phoneNumber": "0771234567",
                                  "district": "Colombo",
                                  "city": "Maharagama"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.phone").value("0771234567"))
                .andExpect(jsonPath("$.district").value("Colombo"))
                .andExpect(jsonPath("$.city").value("Maharagama"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.documentsVerified").value(false));

        ArgumentCaptor<ProfileUpdateRequest> requestCaptor = ArgumentCaptor.forClass(ProfileUpdateRequest.class);
        verify(profileService).updateCurrentUserProfile(requestCaptor.capture());
        ProfileUpdateRequest capturedRequest = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("0771234567", capturedRequest.getPhone());
    }

    @Test
    @WithMockUser(username = "current@example.com", roles = "CUSTOMER")
    void getProfileReturnsPersistedFields() throws Exception {
        ProfileResponse response = ProfileResponse.builder()
                .id("user-1")
                .name("Current User")
                .email("current@example.com")
                .phone("0710000000")
                .district("Colombo")
                .city("Colombo")
                .role("CUSTOMER")
                .documentsVerified(true)
                .build();

        when(profileService.getCurrentUserProfile()).thenReturn(response);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.name").value("Current User"))
                .andExpect(jsonPath("$.email").value("current@example.com"))
                .andExpect(jsonPath("$.phone").value("0710000000"))
                .andExpect(jsonPath("$.district").value("Colombo"))
                .andExpect(jsonPath("$.city").value("Colombo"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.documentsVerified").value(true));
    }

    @Test
    @WithMockUser(username = "current@example.com", roles = "CUSTOMER")
    void updateProfileFailsValidation() throws Exception {
        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email",
                                  "phone": "",
                                  "district": "",
                                  "city": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(username = "current@example.com", roles = "CUSTOMER")
    void updateProfileFailsWhenEmailAlreadyExists() throws Exception {
        when(profileService.updateCurrentUserProfile(any()))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Email is already in use"));

        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Name",
                                  "email": "taken@example.com",
                                  "phone": "0771234567",
                                  "district": "Colombo",
                                  "city": "Maharagama"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    void updateProfileFailsWhenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Object())))
                .andExpect(status().isUnauthorized());
    }
}

