package com.example.backend.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileResponse {
    String id;
    String name;
    String email;
    String phone;
    String district;
    String city;
    String role;
    boolean documentsVerified;
}

