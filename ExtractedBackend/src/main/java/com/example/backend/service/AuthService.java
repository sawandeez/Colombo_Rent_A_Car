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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        // Business Rule: Registration is restricted to Colombo district residents
        if (!"Colombo".equalsIgnoreCase(request.getDistrict())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration is restricted to Colombo district residents only");
        }

        var user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setDistrict(request.getDistrict());
        user.setCity(request.getCity());

        // Security: Public registration always creates CUSTOMER accounts.
        // Admin accounts are seeded directly into the database (see DataLoader).
        user.setRole(UserRole.CUSTOMER);

        userRepository.save(user);
        log.info("REGISTER - New customer registered: {}", user.getEmail());

        String jwtToken = jwtService.generateToken(buildUserDetails(user));
        AuthResponse response = AuthResponse.builder()
                .token(jwtToken)
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .roles(List.of("ROLE_" + user.getRole().name()))
                .build();
        log.debug("REGISTER_RESPONSE - email={}, role={}, roles={}", response.getEmail(), response.getRole(), response.getRoles());
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        log.info("LOGIN - User logged in: {}", user.getEmail());

        String jwtToken = jwtService.generateToken(buildUserDetails(user));
        AuthResponse response = AuthResponse.builder()
                .token(jwtToken)
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .roles(List.of("ROLE_" + user.getRole().name()))
                .build();
        log.debug("LOGIN_RESPONSE - email={}, role={}, roles={}", response.getEmail(), response.getRole(), response.getRoles());
        return response;
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
