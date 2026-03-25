package com.example.backend.controller;

import com.example.backend.dto.AdminUserResponse;
import com.example.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Fetch customer basic details by userId.
     * Admin-only endpoint for booking admin panel to replace "Unknown Customer" labels.
     *
     * @param userId user ID to fetch
     * @return AdminUserResponse with id/name/email/username
     */
    @GetMapping(value = "/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(adminUserService.getUserById(userId));
    }
}

