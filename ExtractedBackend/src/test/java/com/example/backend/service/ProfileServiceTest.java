package com.example.backend.service;

import com.example.backend.dto.ProfileResponse;
import com.example.backend.dto.ProfileUpdateRequest;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateCurrentUserProfilePersistsAndReturnsUpdatedProfile() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("current@example.com", "n/a"));

        User current = new User();
        current.setId("u-1");
        current.setEmail("current@example.com");
        current.setPassword("encoded-password");
        current.setRole(UserRole.CUSTOMER);
        current.setDocumentsVerified(true);

        when(userRepository.findByEmail("current@example.com")).thenReturn(Optional.of(current));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("Updated Name");
        request.setEmail("updated@example.com");
        request.setPhone("0771234567");
        request.setDistrict("Colombo");
        request.setCity("Nugegoda");
        request.setAddress("14 Temple Road");

        ProfileResponse response = profileService.updateCurrentUserProfile(request);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertEquals("Updated Name", savedUser.getName());
        assertEquals("updated@example.com", savedUser.getEmail());
        assertEquals("0771234567", savedUser.getPhone());
        assertEquals("14 Temple Road", savedUser.getAddress());
        assertEquals("Colombo", savedUser.getDistrict());
        assertEquals("Nugegoda", savedUser.getCity());

        // Unchanged fields required by the endpoint policy.
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(UserRole.CUSTOMER, savedUser.getRole());
        assertTrue(savedUser.isDocumentsVerified());

        assertEquals("u-1", response.getId());
        assertEquals("Updated Name", response.getName());
        assertEquals("updated@example.com", response.getEmail());
        assertEquals("0771234567", response.getPhone());
        assertEquals("Colombo", response.getDistrict());
        assertEquals("Nugegoda", response.getCity());
        assertEquals("CUSTOMER", response.getRole());
        assertTrue(response.isDocumentsVerified());
    }

    @Test
    void updateCurrentUserProfileRejectsDuplicateEmail() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("current@example.com", "n/a"));

        User current = new User();
        current.setId("u-1");
        current.setEmail("current@example.com");

        User otherUser = new User();
        otherUser.setId("u-2");
        otherUser.setEmail("taken@example.com");

        when(userRepository.findByEmail("current@example.com")).thenReturn(Optional.of(current));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("Updated Name");
        request.setEmail("taken@example.com");
        request.setPhone("0771234567");
        request.setDistrict("Colombo");
        request.setCity("Nugegoda");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> profileService.updateCurrentUserProfile(request));
        assertEquals(BAD_REQUEST, ex.getStatusCode());
        assertEquals("Email is already in use", ex.getReason());
    }

    @Test
    void updateCurrentUserProfileReturns404WhenAuthenticatedUserRecordMissing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("missing@example.com", "n/a"));

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("Updated Name");
        request.setEmail("updated@example.com");
        request.setPhone("0771234567");
        request.setDistrict("Colombo");
        request.setCity("Nugegoda");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> profileService.updateCurrentUserProfile(request));
        assertEquals(NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }
}


