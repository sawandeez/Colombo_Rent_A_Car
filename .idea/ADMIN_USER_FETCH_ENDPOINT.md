# Admin User Fetch Endpoint Implementation

**Date:** 2026-03-25  
**Goal:** Add admin-only endpoint `GET /api/v1/admin/users/{userId}` to fetch customer basic details for booking admin panel.

---

## Files Created (4 new files, 0 modified)

| File | Purpose |
|------|---------|
| `AdminUserController.java` | New REST controller with GET endpoint |
| `AdminUserService.java` | New service layer for user fetching logic |
| `AdminUserResponse.java` | New response DTO with id/name/email/username |
| `AdminUserControllerTest.java` | New focused test class (4 tests) |

---

## Code Changes

### 1. `src/main/java/com/example/backend/dto/AdminUserResponse.java` (NEW)

```java
package com.example.backend.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminUserResponse {
    String id;
    String name;
    String email;
    String username;
}
```

**Why:** Exposes only non-sensitive customer fields for admin consumption. Uses immutable `@Value` + `@Builder` for DTO best practice.

---

### 2. `src/main/java/com/example/backend/service/AdminUserService.java` (NEW)

```java
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
```

**Why:** 
- Isolated service for admin user operations.
- Uses existing `UserRepository.findById()` (no new repo methods needed).
- Throws `404 NOT_FOUND` if user not found.
- Maps User entity to response DTO (does NOT expose password, tokens, internal flags).

---

### 3. `src/main/java/com/example/backend/controller/AdminUserController.java` (NEW)

```java
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
```

**Why:**
- Endpoint path: `GET /api/v1/admin/users/{userId}` (exact requirement).
- `@RequestMapping("/api/v1/admin")` scopes all routes under admin namespace.
- Security: Already protected by `SecurityConfiguration` line 58: `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`.
- Returns `ResponseEntity<AdminUserResponse>` with JSON content type.
- Delegates to service for business logic.

---

### 4. `src/test/java/com/example/backend/controller/AdminUserControllerTest.java` (NEW)

```java
package com.example.backend.controller;

import com.example.backend.dto.AdminUserResponse;
import com.example.backend.security.JwtService;
import com.example.backend.security.SecurityConfiguration;
import com.example.backend.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@Import(SecurityConfiguration.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanFetchExistingUserById() throws Exception {
        AdminUserResponse userResponse = AdminUserResponse.builder()
                .id("user-123")
                .name("John Doe")
                .email("john@example.com")
                .username("john@example.com")
                .build();

        when(adminUserService.getUserById("user-123")).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/admin/users/user-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.username").value("john@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminGetUserReturns404ForUnknownUserId() throws Exception {
        when(adminUserService.getUserById("unknown-id"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/v1/admin/users/unknown-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void nonAdminGetsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/user-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGetUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/user-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
```

**Tests Covered:**
1. ✅ Admin can fetch existing user by ID → 200 with response fields
2. ✅ Unknown userId → 404 with clear message
3. ✅ Non-admin (CUSTOMER role) → 403 Forbidden
4. ✅ Unauthenticated → 401 Unauthorized

---

## Why No Unrelated Backend Logic Was Changed

- **No profile/auth changes:** Login/register/password flows untouched.
- **No booking changes:** Booking service/controller/domain logic unchanged.
- **No vehicle changes:** Vehicle CRUD/search logic unchanged.
- **No document changes:** Document upload/download logic unchanged.
- **No security config changes:** Existing `hasRole("ADMIN")` rule on `/api/v1/admin/**` already protects this endpoint.
- **No repo changes:** Used existing `UserRepository.findById()` — no new queries added.
- **No user entity changes:** User model remains as-is.

---

## API Contract

### Request
```
GET /api/v1/admin/users/{userId}
Authorization: Bearer <admin-jwt-token>
```

### Response (200 OK)
```json
{
  "id": "user-123",
  "name": "John Doe",
  "email": "john@example.com",
  "username": "john@example.com"
}
```

### Error Responses
- **401 Unauthorized** — no JWT token or invalid token
- **403 Forbidden** — authenticated but not ADMIN role
- **404 Not Found** — user ID does not exist in database

---

## How This Solves the Frontend Requirement

Frontend booking admin panel currently shows "Unknown Customer" for booking userId. With this endpoint:

1. When admin views a booking with `userId: "user-123"`
2. Frontend calls `GET /api/v1/admin/users/user-123`
3. Backend returns `{ id, name, email, username }`
4. Frontend displays real customer name instead of "Unknown Customer"

---

## Minimal Implementation Checklist

- [x] New endpoint `GET /api/v1/admin/users/{userId}`
- [x] Admin-only security (reused existing config)
- [x] 401/403/404 error handling
- [x] Response DTO with required fields (id, name, email, username)
- [x] No sensitive field exposure (password, tokens not included)
- [x] Isolated service layer
- [x] 4 focused tests covering all scenarios
- [x] No changes to unrelated business logic

