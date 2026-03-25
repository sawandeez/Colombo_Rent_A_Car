# Mandatory Documents + Security Refactor Report

Date: 2026-03-24

## Scope Completed

Implemented a backend refactor for Spring Boot 3 + Spring Security 6 + MongoDB to enforce mandatory user documents for booking creation, remove conflicting booking create mappings, and align endpoints/security with the `/api/v1` contract.

## What Was Changed

### 1) Booking creation conflict removed

- Removed multipart booking creation from `POST /api/v1/bookings`.
- Kept a single booking create endpoint:
  - `POST /api/v1/bookings` (JSON body: `BookingCreateRequest`)

### 2) Mandatory document enforcement

- Booking creation now requires stored user documents for:
  - `NIC_FRONT`
  - `DRIVING_LICENSE`
- If either is missing, booking creation fails with HTTP 400 and a clear message:
  - `Missing mandatory document: NIC_FRONT`
  - `Missing mandatory document: DRIVING_LICENSE`

### 3) Document storage moved to MongoDB (GridFS)

- Added GridFS-backed storage service for file bytes.
- Added metadata persistence in MongoDB:
  - `id`
  - `ownerUserId`
  - `category` (`NIC_FRONT`, `DRIVING_LICENSE`, `OTHER`)
  - `originalFilename`
  - `contentType`
  - `size`
  - `createdAt`
  - `fileId` (GridFS file reference)

### 4) Document API split by responsibility (SOLID)

- Added user-scoped document controller:
  - Base: `/api/v1/users/me/documents`
  - Upload/list/download/delete for authenticated owner
- Added admin-scoped document controller:
  - Base: `/api/v1/admin/users/{userId}/documents`
  - List/download/delete for admin

### 5) Booking model now stores document IDs

- Replaced booking fields from path-based to ID-based references:
  - `nicFrontDocumentId`
  - `drivingLicenseDocumentId`

### 6) Admin bookings API aligned

- Updated admin booking endpoints to:
  - `GET /api/v1/admin/bookings?status=&search=&page=&size=`
  - `PATCH /api/v1/admin/bookings/{id}/approve`
  - `PATCH /api/v1/admin/bookings/{id}/reject` (JSON `{ "reason": "..." }`)

### 7) Security rules and matcher safety

- Security matcher rules updated to avoid invalid pattern usage and endpoint conflicts.
- Key matcher coverage now:
  - Permit all: auth routes, swagger, `/api/health`, `/api/db-status`, public vehicle GETs
  - Authenticated: `/api/v1/bookings/**`, `/api/v1/users/me/documents/**`
  - Admin only: `/api/v1/admin/**`

### 8) Error handling

- Added explicit `ResponseStatusException` handling in global exception handler for consistent API error payloads.

## Files Added

- `src/main/java/com/example/backend/model/DocumentCategory.java`
- `src/main/java/com/example/backend/model/UserDocument.java`
- `src/main/java/com/example/backend/repository/UserDocumentRepository.java`
- `src/main/java/com/example/backend/service/DocumentStorageService.java`
- `src/main/java/com/example/backend/service/UserDocumentService.java`
- `src/main/java/com/example/backend/controller/UserDocumentController.java`
- `src/main/java/com/example/backend/controller/AdminUserDocumentController.java`
- `src/main/java/com/example/backend/dto/UserDocumentMetadataResponse.java`
- `src/main/java/com/example/backend/dto/BookingRejectRequest.java`

## Files Updated

- `src/main/java/com/example/backend/controller/BookingController.java`
- `src/main/java/com/example/backend/controller/AdminBookingController.java`
- `src/main/java/com/example/backend/controller/AuthController.java`
- `src/main/java/com/example/backend/controller/GlobalExceptionHandler.java`
- `src/main/java/com/example/backend/service/BookingService.java`
- `src/main/java/com/example/backend/security/SecurityConfiguration.java`
- `src/main/java/com/example/backend/model/Booking.java`
- `src/main/java/com/example/backend/dto/BookingResponse.java`
- `src/main/java/com/example/backend/mapper/BookingMapper.java`
- `src/test/java/com/example/backend/controller/BookingDocumentIntegrationTest.java`

## Test Coverage Updated

`BookingDocumentIntegrationTest` now includes scenarios for:

- User uploads document and downloads it.
- User cannot access admin document endpoint (403).
- Booking create fails (400) when mandatory docs are missing.
- Booking create succeeds when docs are present.
- Admin can list bookings and approve/reject.

`SecurityConfigurationStartupTest` remains as startup smoke validation for security filter chain loading.

## Verification Status

- Static checks via IDE error inspection were clean for modified source files (warnings may remain for usage/indexing depending on IDE state).
- Runtime test execution could not be completed in this environment because Maven CLI/wrapper was not available from shell context.

## Suggested Local Verification

Run from `ExtractedBackend`:

```powershell
mvn -Dtest=BookingDocumentIntegrationTest,SecurityConfigurationStartupTest test
```

If Maven wrapper is available in your local copy:

```powershell
.\mvnw.cmd -Dtest=BookingDocumentIntegrationTest,SecurityConfigurationStartupTest test
```

