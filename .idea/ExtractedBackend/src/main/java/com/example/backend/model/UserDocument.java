package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "user_documents")
public class UserDocument {

	@Id
	private String id;

	@Indexed
	private String ownerUserId;

	private DocumentCategory category;

	private String originalFilename;
	private String contentType;
	private long size;
	private String fileId;
	private LocalDateTime createdAt;
}

