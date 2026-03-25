package com.example.backend.controller;

import com.example.backend.dto.UserDocumentMetadataResponse;
import com.example.backend.model.DocumentCategory;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/documents")
@RequiredArgsConstructor
public class UserDocumentController {

    private final UserDocumentService userDocumentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDocumentMetadataResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("category") DocumentCategory category) {
        return ResponseEntity.ok(userDocumentService.uploadForCurrentUser(file, category));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserDocumentMetadataResponse>> listMine() {
        return ResponseEntity.ok(userDocumentService.listCurrentUserDocuments());
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<Resource> downloadMine(@PathVariable String documentId) {
        UserDocumentService.DownloadDocumentResponse doc = userDocumentService.downloadCurrentUserDocument(documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(doc.fileName()).build().toString())
                .body(doc.resource());
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteMine(@PathVariable String documentId) {
        userDocumentService.deleteCurrentUserDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}

