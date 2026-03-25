package com.example.backend.controller;

import com.example.backend.dto.UserDocumentMetadataResponse;
import com.example.backend.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users/{userId}/documents")
@RequiredArgsConstructor
public class AdminUserDocumentController {

    private final UserDocumentService userDocumentService;

    @GetMapping
    public ResponseEntity<List<UserDocumentMetadataResponse>> listByUser(@PathVariable String userId) {
        return ResponseEntity.ok(userDocumentService.listUserDocuments(userId));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<Resource> downloadByUser(
            @PathVariable String userId,
            @PathVariable String documentId) {
        UserDocumentService.DownloadDocumentResponse doc = userDocumentService.downloadUserDocumentAsAdmin(userId, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(doc.fileName()).build().toString())
                .body(doc.resource());
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteByUser(
            @PathVariable String userId,
            @PathVariable String documentId) {
        userDocumentService.deleteUserDocumentAsAdmin(userId, documentId);
        return ResponseEntity.noContent().build();
    }
}

