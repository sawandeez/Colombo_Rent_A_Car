package com.example.backend.service;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileBackfillService implements ApplicationRunner {

    private static final String USERS_COLLECTION = "users";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // Backfill legacy aliases once per startup so old rows continue to work.
        backfillAlias("phoneNumber", "phone");
        backfillAlias("districtName", "district");
        backfillAlias("cityName", "city");
    }

    private void backfillAlias(String sourceField, String targetField) {
        for (Document userDoc : mongoTemplate.getCollection(USERS_COLLECTION).find()) {
            String currentValue = trimToNull(userDoc.getString(targetField));
            String sourceValue = trimToNull(userDoc.getString(sourceField));

            if (currentValue == null && sourceValue != null) {
                UpdateResult result = mongoTemplate.getCollection(USERS_COLLECTION)
                        .updateOne(new Document("_id", userDoc.get("_id")),
                                new Document("$set", new Document(targetField, sourceValue)));
                if (result.getModifiedCount() > 0) {
                    log.info("BACKFILL - populated {} from {} for user {}", targetField, sourceField, userDoc.get("_id"));
                }
            }

            if (currentValue != null) {
                String normalized = currentValue.trim();
                if (!Objects.equals(currentValue, normalized)) {
                    mongoTemplate.getCollection(USERS_COLLECTION)
                            .updateOne(new Document("_id", userDoc.get("_id")),
                                    new Document("$set", new Document(targetField, normalized)));
                }
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

