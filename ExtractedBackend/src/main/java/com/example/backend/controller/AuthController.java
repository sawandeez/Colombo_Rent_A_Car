package com.example.backend.controller;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.ProfileResponse;
import com.example.backend.dto.ProfileUpdateRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AuthService;
import com.example.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final UserRepository userRepository;
    private final ProfileService profileService;

    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    @GetMapping("/api/v1/auth/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        return ResponseEntity.ok(buildAuthenticatedProfileResponse());
    }

    @GetMapping(value = "/api/v1/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getCurrentUserProfile());
    }

    @PutMapping(value = "/api/v1/profile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateCurrentUserProfile(request));
    }

    private AuthResponse buildAuthenticatedProfileResponse() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String email = auth.getName() == null ? null : auth.getName().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(resolvePhone(user))
                .district(resolveDistrict(user))
                .city(resolveCity(user))
                .role(user.getRole().name())
                .roles(List.of("ROLE_" + user.getRole().name()))
                .documentsVerified(user.isDocumentsVerified())
                .tokenType("Bearer")
                .build();
    }

    private String resolvePhone(User user) {
        return firstNonBlank(normalizeText(user.getPhone()), normalizeText(readLegacyStringField(user, "phoneNumber")));
    }

    private String resolveDistrict(User user) {
        return firstNonBlank(normalizeText(user.getDistrict()), normalizeText(readLegacyStringField(user, "districtName")));
    }

    private String resolveCity(User user) {
        return firstNonBlank(normalizeText(user.getCity()), normalizeText(readLegacyStringField(user, "cityName")));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String readLegacyStringField(User user, String fieldName) {
        try {
            Field field = User.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(user);
            return value instanceof String str ? str : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

}
