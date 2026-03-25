package com.example.backend.service;

import com.example.backend.dto.UserDocumentMetadataResponse;
import com.example.backend.model.DocumentCategory;
import com.example.backend.model.User;
import com.example.backend.model.UserDocument;
import com.example.backend.model.UserRole;
import com.example.backend.repository.UserDocumentRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.APPLICATION_PDF_VALUE
    );

    /** Extensions that are trusted even when the part arrives as application/octet-stream or without a content type. */
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            ".jpg",  MediaType.IMAGE_JPEG_VALUE,
            ".jpeg", MediaType.IMAGE_JPEG_VALUE,
            ".png",  MediaType.IMAGE_PNG_VALUE,
            ".pdf",  MediaType.APPLICATION_PDF_VALUE
    );

    private final UserDocumentRepository userDocumentRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;

    @Value("${app.upload.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Transactional
    public UserDocumentMetadataResponse uploadForCurrentUser(MultipartFile file, DocumentCategory category) {
        User user = getAuthenticatedUser();
        return uploadForUser(user.getId(), file, category);
    }

    @Transactional
    public UserDocumentMetadataResponse uploadForUser(String userId, MultipartFile file, DocumentCategory category) {
        validateUpload(file);
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }
        ensureUserExists(userId);

        if (isMandatoryCategory(category)) {
            userDocumentRepository.findByOwnerUserIdAndCategory(userId, category)
                    .ifPresent(this::deleteDocumentInternal);
        }

        String contentType = resolveEffectiveContentType(file);

        String fileId;
        try {
            fileId = documentStorageService.store(
                    file.getInputStream(),
                    resolveFilename(file),
                    contentType,
                    Map.of("ownerUserId", userId, "category", category.name()));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store document", ex);
        }

        UserDocument doc = new UserDocument();
        doc.setOwnerUserId(userId);
        doc.setCategory(category);
        doc.setOriginalFilename(resolveFilename(file));
        doc.setContentType(contentType);
        doc.setSize(file.getSize());
        doc.setFileId(fileId);
        doc.setCreatedAt(LocalDateTime.now());

        UserDocument saved = userDocumentRepository.save(doc);
        if (saved.getId() == null || saved.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document id was not generated");
        }
        return toDto(saved);
    }

    public List<UserDocumentMetadataResponse> listCurrentUserDocuments() {
        User user = getAuthenticatedUser();
        return listUserDocuments(user.getId());
    }

    public List<UserDocumentMetadataResponse> listUserDocuments(String userId) {
        ensureUserExists(userId);
        return userDocumentRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public DownloadDocumentResponse downloadCurrentUserDocument(String documentId) {
        User user = getAuthenticatedUser();
        UserDocument document = findDocumentOwnedByUser(documentId, user.getId());
        return loadDownloadResponse(document);
    }

    public DownloadDocumentResponse downloadUserDocumentAsAdmin(String userId, String documentId) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

        ensureUserExists(userId);
        UserDocument document = findDocumentOwnedByUser(documentId, userId);
        return loadDownloadResponse(document);
    }

    @Transactional
    public void deleteCurrentUserDocument(String documentId) {
        User user = getAuthenticatedUser();
        UserDocument document = findDocumentOwnedByUser(documentId, user.getId());
        deleteDocumentInternal(document);
    }

    @Transactional
    public void deleteUserDocumentAsAdmin(String userId, String documentId) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

        ensureUserExists(userId);
        UserDocument document = findDocumentOwnedByUser(documentId, userId);
        deleteDocumentInternal(document);
    }

    public UserDocument requireMandatoryDocument(String ownerUserId, DocumentCategory category) {
        return userDocumentRepository.findByOwnerUserIdAndCategory(ownerUserId, category)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing mandatory document: " + category.name()));
    }

    private UserDocument findDocumentOwnedByUser(String documentId, String ownerUserId) {
        UserDocument document = userDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!ownerUserId.equals(document.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this document");
        }
        return document;
    }

    private DownloadDocumentResponse loadDownloadResponse(UserDocument document) {
        DocumentStorageService.DocumentResource file = documentStorageService.load(document.getFileId());
        return new DownloadDocumentResponse(file.resource(), file.contentType(), document.getOriginalFilename());
    }

    private void deleteDocumentInternal(UserDocument document) {
        documentStorageService.delete(document.getFileId());
        userDocumentRepository.delete(document);
    }

    private User ensureUserExists(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }

        String normalized = normalizeContentType(file.getContentType());
        boolean byMime = ALLOWED_CONTENT_TYPES.contains(normalized);
        boolean byOctetStream = normalized.isEmpty()
                || normalized.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        if (byMime) {
            // Fine — explicit allowed MIME.
        } else if (byOctetStream) {
            // Part arrived without a real content-type — accept only if extension is trusted.
            String ext = fileExtension(resolveFilename(file));
            if (!ALLOWED_EXTENSIONS.containsKey(ext)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only jpg/jpeg/png/pdf allowed");
            }
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only jpg/jpeg/png/pdf allowed");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File size exceeds the maximum allowed size of " + maxFileSizeBytes + " bytes");
        }
    }

    /**
     * Returns the MIME type to be stored.
     * Falls back to extension-based lookup when the part content-type is null/blank/octet-stream.
     */
    private String resolveEffectiveContentType(MultipartFile file) {
        String normalized = normalizeContentType(file.getContentType());
        if (ALLOWED_CONTENT_TYPES.contains(normalized)) {
            return normalized;
        }
        // octet-stream or empty — derive from extension
        String ext = fileExtension(resolveFilename(file));
        return ALLOWED_EXTENSIONS.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    /** Returns the lower-cased extension including the dot, e.g. ".pdf", or "" if none. */
    private String fileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : "";
    }

    private boolean isMandatoryCategory(DocumentCategory category) {
        return category == DocumentCategory.NIC_FRONT || category == DocumentCategory.DRIVING_LICENSE;
    }

    private String resolveFilename(MultipartFile file) {
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return "document";
        }
        return file.getOriginalFilename();
    }

    private UserDocumentMetadataResponse toDto(UserDocument doc) {
        return UserDocumentMetadataResponse.builder()
                .id(doc.getId())
                .ownerUserId(doc.getOwnerUserId())
                .category(doc.getCategory())
                .originalFilename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .size(doc.getSize())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    public record DownloadDocumentResponse(org.springframework.core.io.Resource resource, String contentType, String fileName) {
    }
}

