package com.example.backend.service;

import com.example.backend.dto.AdminUserResponse;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    /**
     * Fetch user by ID for admin use.
     * Returns basic public customer details (id, name, email, username).
     * Does not expose sensitive fields (password, tokens, flags).
     *
     * @param userId user ID to fetch
     * @return AdminUserResponse with id/name/email/username
     * @throws ResponseStatusException 404 if user not found
     */
    public AdminUserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        return toAdminUserResponse(user);
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .username(user.getEmail()) // Username field maps to email for now
                .build();
    }
}

