package com.example.backend.dto;

import com.example.backend.model.DocumentCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentMetadataResponse {
    private String id;
    private String ownerUserId;
    private DocumentCategory category;
    private String originalFilename;
    private String contentType;
    private long size;
    private LocalDateTime createdAt;
}

