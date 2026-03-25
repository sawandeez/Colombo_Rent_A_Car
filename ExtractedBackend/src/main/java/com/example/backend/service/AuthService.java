package com.example.backend.service;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizeText(request.getPhone());
        String district = normalizeText(request.getDistrict());
        String city = normalizeText(request.getCity());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone is required");
        }
        if (district == null || district.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "district is required");
        }
        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "city is required");
        }

        // Business Rule: Registration is restricted to Colombo district residents
        if (!"Colombo".equalsIgnoreCase(district)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration is restricted to Colombo district residents only");
        }

        var user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(normalizeText(request.getName()));
        user.setPhone(phone);
        user.setDistrict(district);
        user.setCity(city);

        // Security: Public registration always creates CUSTOMER accounts.
        // Admin accounts are seeded directly into the database (see DataLoader).
        user.setRole(UserRole.CUSTOMER);

        User savedUser = userRepository.save(user);
        log.info("REGISTER - New customer registered: {}", user.getEmail());

        String jwtToken = jwtService.generateToken(buildUserDetails(savedUser));
        AuthResponse response = buildAuthResponse(savedUser, jwtToken);
        log.debug("REGISTER_RESPONSE - email={}, role={}, roles={}", response.getEmail(), response.getRole(), response.getRoles());
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        var user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        log.info("LOGIN - User logged in: {}", user.getEmail());

        String jwtToken = jwtService.generateToken(buildUserDetails(user));
        AuthResponse response = buildAuthResponse(user, jwtToken);
        log.debug("LOGIN_RESPONSE - email={}, role={}, roles={}", response.getEmail(), response.getRole(), response.getRoles());
        return response;
    }

    private AuthResponse buildAuthResponse(User user, String jwtToken) {
        return AuthResponse.builder()
                .id(user.getId())
                .token(jwtToken)
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .phone(resolvePhone(user))
                .district(resolveDistrict(user))
                .city(resolveCity(user))
                .role(user.getRole().name())
                .roles(List.of("ROLE_" + user.getRole().name()))
                .documentsVerified(user.isDocumentsVerified())
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

    private String normalizeEmail(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
