package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String id;
    private String token;
    private String accessToken;
    @JsonProperty("tokenType")
    private String tokenType = "Bearer";
    private String email;
    private String role;
    private List<String> roles;
    private String name;
    private String phone;
    private String district;
    private String city;
    private boolean documentsVerified;
}
