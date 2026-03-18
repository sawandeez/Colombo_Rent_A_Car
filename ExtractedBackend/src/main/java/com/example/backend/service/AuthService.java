package com.carrental.system.service;

import com.carrental.system.domain.User;
import com.carrental.system.domain.UserRole;
import com.carrental.system.dto.AuthResponse;
import com.carrental.system.dto.LoginRequest;
import com.carrental.system.dto.RegisterRequest;
import com.carrental.system.exception.BusinessRuleException;
import com.carrental.system.repository.UserRepository;
import com.carrental.system.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("An account with this email already exists");
        }

        // Business Rule: Registration is restricted to Colombo district residents
        if (!"Colombo".equalsIgnoreCase(request.getDistrict())) {
            throw new BusinessRuleException("Registration is restricted to Colombo district residents only");
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
        auditService.logAction("REGISTER", user.getEmail(), "New customer registered");

        String jwtToken = jwtService.generateToken(buildUserDetails(user));
        return new AuthResponse(jwtToken, user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials"));

        auditService.logAction("LOGIN", user.getEmail(), "User logged in");

        String jwtToken = jwtService.generateToken(buildUserDetails(user));
        return new AuthResponse(jwtToken, user.getRole().name());
    }

    /**
     * Builds a Spring Security UserDetails from a User domain object.
     * Centralised here to avoid duplication across register/login.
     */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
