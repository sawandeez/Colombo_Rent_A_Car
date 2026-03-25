# Fix: Document Upload 415 Unsupported Media Type

**Date:** 2026-03-24  
**Root cause:**  
Some browsers and HTTP clients send the individual file part inside a `multipart/form-data` request with its own `Content-Type: application/octet-stream` header.  
Spring MVC's `@RequestPart` binding sees that per-part content-type, and if the method signature or validation code does not account for `application/octet-stream`, the framework throws `HttpMediaTypeNotSupportedException` → **HTTP 415**.

---

## Files Changed

| File | Why |
|---|---|
| `controller/UserDocumentController.java` | Ensure `@RequestPart` + correct `consumes`/`produces` _(was already fixed in a prior pass; no further change)_ |
| `service/UserDocumentService.java` | Relax MIME validation to accept `application/octet-stream` / null when filename extension is trusted; add `resolveEffectiveContentType` |
| `controller/GlobalExceptionHandler.java` | Add explicit `HttpMediaTypeNotSupportedException` handler returning a clear JSON 415 body |
| `resources/application.properties` | Add `spring.servlet.multipart.enabled=true` _(was already done; no further change)_ |

---

## 1 · `UserDocumentController.java` (no new changes required)

The controller already has the correct form after the previous fix:

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
             produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<UserDocumentMetadataResponse> upload(
        @RequestPart("file")     MultipartFile file,
        @RequestPart("category") DocumentCategory category) {
    return ResponseEntity.ok(userDocumentService.uploadForCurrentUser(file, category));
}
```

**Why `@RequestPart` instead of `@RequestParam`:**  
`@RequestParam` tries to read the parameter as a plain form field or URL parameter; Spring occasionally resolves the part's own `Content-Type` against the method parameter type and rejects `application/octet-stream`.  
`@RequestPart` tells Spring "this is a named multipart part" and delegates type conversion itself, avoiding spurious 415 errors.

---

## 2 · `UserDocumentService.java`

### What changed

**Before** — `validateUpload` rejected anything that was not exactly `image/jpeg`, `image/png`, or `application/pdf`:

```java
if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
    throw new ResponseStatusException(BAD_REQUEST,
        "Unsupported file content type ...");
}
```

This caused a `400` for `application/octet-stream`, but only after the framework had already routed the request. If the dispatcher itself saw the part type first, it could produce a **415 before the service was even called**.

**After** — Two-step validation + effective content-type resolution:

```java
// MIME allow-list
private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
    "image/jpeg", "image/png", "application/pdf"
);

// Extension fallback (used when part arrives as application/octet-stream or without a type)
private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
    ".jpg",  "image/jpeg",
    ".jpeg", "image/jpeg",
    ".png",  "image/png",
    ".pdf",  "application/pdf"
);
```

**`validateUpload` logic:**

| Part content-type | Filename extension | Result |
|---|---|---|
| `image/jpeg` / `image/png` / `application/pdf` | any | ✅ Accept |
| `application/octet-stream` or empty / null | `.jpg` `.jpeg` `.png` `.pdf` | ✅ Accept |
| `application/octet-stream` or empty / null | anything else | ❌ 400 "Only jpg/jpeg/png/pdf allowed" |
| any other explicit MIME | any | ❌ 400 "Only jpg/jpeg/png/pdf allowed" |

**`resolveEffectiveContentType`** — once the file passes validation, the actual MIME type written to the database is resolved correctly:

```java
private String resolveEffectiveContentType(MultipartFile file) {
    String normalized = normalizeContentType(file.getContentType());
    if (ALLOWED_CONTENT_TYPES.contains(normalized)) {
        return normalized;                     // real MIME — use it directly
    }
    // octet-stream / null — infer from extension
    String ext = fileExtension(resolveFilename(file));
    return ALLOWED_EXTENSIONS.getOrDefault(ext, "application/octet-stream");
}
```

This means a file uploaded as `application/octet-stream` but named `nic.pdf` is stored with `contentType = "application/pdf"` — correct for download responses.

**`fileExtension` helper:**

```java
private String fileExtension(String filename) {
    if (filename == null) return "";
    int dot = filename.lastIndexOf('.');
    return dot >= 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : "";
}
```

### Size validation (unchanged behaviour, still present)

```java
if (file.getSize() > maxFileSizeBytes) {
    throw new ResponseStatusException(BAD_REQUEST,
        "File size exceeds the maximum allowed size of " + maxFileSizeBytes + " bytes");
}
```

`maxFileSizeBytes` is bound from `app.upload.max-file-size-bytes` (defaults to `5242880` = 5 MB).

---

## 3 · `GlobalExceptionHandler.java`

### What was added

```java
@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(
        HttpMediaTypeNotSupportedException ex,
        HttpServletRequest request) {
    return buildError(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported Content-Type. This endpoint requires multipart/form-data " +
            "with fields file and category.",
            request,
            null);
}
```

**Why this matters:**  
Without this handler, Spring Boot's default error handling returns a generic HTML or plain-text body for 415. With it, any remaining framework-level 415 (e.g., client accidentally sending `application/json` to this endpoint) produces a consistent JSON response:

```json
{
  "timestamp": "2026-03-24T10:00:00Z",
  "status": 415,
  "error": "Unsupported Media Type",
  "message": "Unsupported Content-Type. This endpoint requires multipart/form-data with fields file and category.",
  "path": "/api/v1/users/me/documents"
}
```

---

## 4 · `application.properties` (no new changes required)

Already correctly set:

```properties
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=20MB
app.upload.max-file-size-bytes=5242880
```

---

## How This Prevents the 415

| Scenario | Before fix | After fix |
|---|---|---|
| Browser sends `multipart/form-data`; file part is `application/octet-stream` | 415 or 400 depending on Spring version | ✅ 200 — accepted via extension fallback |
| Browser sends `multipart/form-data`; file part is `image/jpeg` | 200 | ✅ 200 |
| Client sends `application/json` body | Raw error page / 415 | ✅ 415 with clear JSON message |
| File is a `.exe` disguised as octet-stream | 200 (no check) / 415 | ✅ 400 "Only jpg/jpeg/png/pdf allowed" |
| File exceeds 5 MB | Spring 413 or service error | ✅ 400 with size message |

---

## Verifying the Fix

### cURL — file part with `application/octet-stream`

```bash
curl -X POST http://localhost:8080/api/v1/users/me/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@nic-front.pdf;type=application/octet-stream" \
  -F "category=NIC_FRONT"
```

Expected response:

```json
{
  "id": "<generated-id>",
  "ownerUserId": "...",
  "category": "NIC_FRONT",
  "originalFilename": "nic-front.pdf",
  "contentType": "application/pdf",
  "size": 12345,
  "createdAt": "2026-03-24T10:00:00"
}
```

### cURL — disallowed file type

```bash
curl -X POST http://localhost:8080/api/v1/users/me/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@virus.exe;type=application/octet-stream" \
  -F "category=NIC_FRONT"
```

Expected response (`400`):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Only jpg/jpeg/png/pdf allowed"
}
```

