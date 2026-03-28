package com.example.backend.service;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.ProfileResponse;
import com.example.backend.dto.ProfileUpdateRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthProfilePersistenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;
    private ProfileService profileService;

    private final Map<String, User> usersByEmail = new HashMap<>();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
        profileService = new ProfileService(userRepository);

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "enc-" + invocation.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(authenticationManager.authenticate(any())).thenAnswer(invocation -> {
            UsernamePasswordAuthenticationToken input = invocation.getArgument(0);
            return new UsernamePasswordAuthenticationToken(input.getPrincipal(), null);
        });

        when(userRepository.existsByEmail(anyString())).thenAnswer(invocation -> usersByEmail.containsKey(invocation.getArgument(0)));
        when(userRepository.findByEmail(anyString())).thenAnswer(invocation -> Optional.ofNullable(usersByEmail.get(invocation.getArgument(0))));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null || user.getId().isBlank()) {
                user.setId("u-" + UUID.randomUUID());
            }
            usersByEmail.values().removeIf(existing -> existing.getId().equals(user.getId()));
            usersByEmail.put(user.getEmail(), user);
            return user;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        usersByEmail.clear();
    }

    @Test
    void registerLoginThenGetProfileKeepsPhoneDistrictFromDatabase() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Customer One");
        registerRequest.setEmail("Customer1@Example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setPhone("0771234567");
        registerRequest.setAge(29);
        registerRequest.setDistrict("Colombo");
        registerRequest.setCity("123 Main Street, Maharagama");

        AuthResponse registerResponse = authService.register(registerRequest);
        assertNotNull(registerResponse.getId());

        User storedUser = usersByEmail.get("customer1@example.com");
        assertNotNull(storedUser);
        assertEquals(29, storedUser.getAge());
        assertEquals("123 Main Street", storedUser.getAddress());
        assertEquals("Maharagama", storedUser.getCity());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("customer1@example.com");
        loginRequest.setPassword("Password@123");

        AuthResponse loginResponse = authService.login(loginRequest);

        assertEquals("0771234567", loginResponse.getPhone());
        assertEquals("Colombo", loginResponse.getDistrict());
        assertEquals("Maharagama", loginResponse.getCity());
        assertEquals("CUSTOMER", loginResponse.getRole());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer1@example.com", "n/a"));

        ProfileResponse profileResponse = profileService.getCurrentUserProfile();

        assertEquals(loginResponse.getId(), profileResponse.getId());
        assertEquals("Customer One", profileResponse.getName());
        assertEquals("customer1@example.com", profileResponse.getEmail());
        assertEquals("0771234567", profileResponse.getPhone());
        assertEquals("Colombo", profileResponse.getDistrict());
        assertEquals("Maharagama", profileResponse.getCity());
        assertEquals("CUSTOMER", profileResponse.getRole());
    }

    @Test
    void registerWithExplicitAddressAndCityPersistsBothSafely() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Customer Address");
        registerRequest.setEmail("customer.address@example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setPhone("+94771234567");
        registerRequest.setAge(31);
        registerRequest.setDistrict("Colombo");
        registerRequest.setCity("Nugegoda");
        registerRequest.setAddress("42 Lake Road");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response.getId());
        User storedUser = usersByEmail.get("customer.address@example.com");
        assertNotNull(storedUser);
        assertEquals(31, storedUser.getAge());
        assertEquals("42 Lake Road", storedUser.getAddress());
        assertEquals("Nugegoda", storedUser.getCity());
    }

    @Test
    void updateProfileThenReloginStillReturnsUpdatedPhoneDistrictCity() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Customer Two");
        registerRequest.setEmail("customer2@example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setPhone("0700000000");
        registerRequest.setDistrict("Colombo");
        registerRequest.setCity("Colombo");

        authService.register(registerRequest);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer2@example.com", "n/a"));

        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setName("Customer Two Updated");
        updateRequest.setEmail("customer2.updated@example.com");
        updateRequest.setPhone("0711111111");
        updateRequest.setDistrict("Colombo");
        updateRequest.setCity("Nugegoda");
        updateRequest.setAddress("17 Temple Road");

        ProfileResponse updatedProfile = profileService.updateCurrentUserProfile(updateRequest);
        assertEquals("0711111111", updatedProfile.getPhone());
        assertEquals("Nugegoda", updatedProfile.getCity());
        assertEquals("17 Temple Road", usersByEmail.get("customer2.updated@example.com").getAddress());

        SecurityContextHolder.clearContext(); // logout simulation

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("customer2.updated@example.com");
        loginRequest.setPassword("Password@123");

        AuthResponse loginResponse = authService.login(loginRequest);
        assertEquals("0711111111", loginResponse.getPhone());
        assertEquals("Colombo", loginResponse.getDistrict());
        assertEquals("Nugegoda", loginResponse.getCity());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer2.updated@example.com", "n/a"));

        ProfileResponse profileAfterRelogin = profileService.getCurrentUserProfile();
        assertEquals("0711111111", profileAfterRelogin.getPhone());
        assertEquals("Colombo", profileAfterRelogin.getDistrict());
        assertEquals("Nugegoda", profileAfterRelogin.getCity());
        assertTrue(profileAfterRelogin.getEmail().contains("updated"));
    }
}


