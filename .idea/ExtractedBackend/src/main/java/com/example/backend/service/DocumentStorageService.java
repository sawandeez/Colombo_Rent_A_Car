package com.example.backend.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final GridFsTemplate gridFsTemplate;

    public String store(InputStream inputStream, String filename, String contentType, Map<String, Object> metadata) {
        Document meta = new Document();
        if (metadata != null) {
            meta.putAll(metadata);
        }
        ObjectId fileId = gridFsTemplate.store(inputStream, filename, contentType, meta);
        return fileId.toHexString();
    }

    public DocumentResource load(String fileId) {
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(toObjectId(fileId))));
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored file not found");
        }

        GridFsResource resource = gridFsTemplate.getResource(file);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored file not found");
        }

        String contentType = null;
        if (file.getMetadata() != null) {
            contentType = file.getMetadata().getString("contentType");
            if (contentType == null || contentType.isBlank()) {
                contentType = file.getMetadata().getString("_contentType");
            }
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new DocumentResource(resource, contentType, file.getFilename());
    }

    public void delete(String fileId) {
        ObjectId objectId = toObjectId(fileId);
        GridFSFile existing = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(objectId)));
        if (existing == null) {
            return;
        }
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(objectId)));
    }

    private ObjectId toObjectId(String fileId) {
        try {
            return new ObjectId(fileId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored file not found");
        }
    }

    public record DocumentResource(org.springframework.core.io.Resource resource, String contentType, String fileName) {
    }
}

