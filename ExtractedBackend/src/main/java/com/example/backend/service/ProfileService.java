package com.example.backend.service;

import com.example.backend.dto.ProfileResponse;
import com.example.backend.dto.ProfileUpdateRequest;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentUserProfile() {
        return toProfileResponse(getAuthenticatedUser());
    }

    @Transactional
    public ProfileResponse updateCurrentUserProfile(ProfileUpdateRequest request) {
        User currentUser = getAuthenticatedUser();

        String requestedEmail = normalizeEmail(request.getEmail());
        if (requestedEmail == null || requestedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (!requestedEmail.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email must be a valid email address");
        }

        String phone = normalizeText(request.getPhone());
        String district = normalizeText(request.getDistrict());
        String city = normalizeText(request.getCity());

        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone is required");
        }
        if (district == null || district.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "district is required");
        }
        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "city is required");
        }

        userRepository.findByEmail(requestedEmail)
                .filter(existing -> !existing.getId().equals(currentUser.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
                });

        currentUser.setName(normalizeText(request.getName()));
        currentUser.setEmail(requestedEmail);
        currentUser.setPhone(phone);
        currentUser.setDistrict(district);
        currentUser.setCity(city);

        User saved = userRepository.save(currentUser);
        return toProfileResponse(saved);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String email = normalizeEmail(auth.getName());
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(resolvePhone(user))
                .district(resolveDistrict(user))
                .city(resolveCity(user))
                .role(user.getRole() == null ? null : user.getRole().name())
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
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}


