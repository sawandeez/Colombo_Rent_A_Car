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

