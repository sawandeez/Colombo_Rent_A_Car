package com.example.backend.repository;

import com.example.backend.model.DocumentCategory;
import com.example.backend.model.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserDocumentRepository extends MongoRepository<UserDocument, String> {

    List<UserDocument> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);

    Optional<UserDocument> findByOwnerUserIdAndCategory(String ownerUserId, DocumentCategory category);
}

