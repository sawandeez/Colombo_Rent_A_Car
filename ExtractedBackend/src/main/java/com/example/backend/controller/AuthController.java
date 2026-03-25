package com.example.backend.controller;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AuthService;
import com.example.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final BookingService bookingService;
    private final UserRepository userRepository;

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

    @GetMapping("/api/v1/profile")
    public ResponseEntity<AuthResponse> getProfile() {
        return ResponseEntity.ok(buildAuthenticatedProfileResponse());
    }

    @GetMapping("/api/users/me/documents/nic-front")
    public ResponseEntity<Resource> getMyNicFront() {
        BookingService.DocumentContent document = bookingService.loadMyNicFrontDocument();
        return buildDownloadResponse(document);
    }

    @GetMapping("/api/users/me/documents/driving-license")
    public ResponseEntity<Resource> getMyDrivingLicense() {
        BookingService.DocumentContent document = bookingService.loadMyDrivingLicenseDocument();
        return buildDownloadResponse(document);
    }

    @GetMapping("/api/admin/users/{userId}/documents/nic-front")
    public ResponseEntity<Resource> getUserNicFrontForAdmin(@PathVariable String userId) {
        BookingService.DocumentContent document = bookingService.loadUserNicFrontDocumentForAdmin(userId);
        return buildDownloadResponse(document);
    }

    @GetMapping("/api/admin/users/{userId}/documents/driving-license")
    public ResponseEntity<Resource> getUserDrivingLicenseForAdmin(@PathVariable String userId) {
        BookingService.DocumentContent document = bookingService.loadUserDrivingLicenseDocumentForAdmin(userId);
        return buildDownloadResponse(document);
    }

    private AuthResponse buildAuthenticatedProfileResponse() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return AuthResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .roles(List.of("ROLE_" + user.getRole().name()))
                .tokenType("Bearer")
                .build();
    }

    private ResponseEntity<Resource> buildDownloadResponse(BookingService.DocumentContent document) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(document.fileName())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(document.resource());
    }
}
