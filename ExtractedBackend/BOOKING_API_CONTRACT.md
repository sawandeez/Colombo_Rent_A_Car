# Booking API Contract (Frontend Alignment)

## Endpoint
- **Method:** `POST`
- **Path:** `/api/v1/bookings`
- **Auth:** `Bearer <JWT>`
- **Role:** `CUSTOMER` (also allowed for `ADMIN`)
- **Content-Type:** `application/json`

## Request JSON (Supported Formats)
### Existing frontend payload
```json
{
  "vehicleId": "veh-123",
  "startDate": "2026-04-10T09:00:00",
  "endDate": "2026-04-12T18:00:00"
}
```

### New-compatible payload
```json
{
  "vehicleId": "veh-123",
  "startDate": "2026-04-10",
  "endDate": "2026-04-12",
  "pickupDateTime": "2026-04-10T00:00:00",
  "returnDateTime": "2026-04-12T00:00:00"
}
```

## Request Field Rules
- `vehicleId` (string, required, non-blank)
- `startDate` (string, required, accepts `yyyy-MM-dd` **or** ISO-8601 datetime)
- `endDate` (string, required, accepts `yyyy-MM-dd` **or** ISO-8601 datetime)
- `pickupDateTime` (optional, ISO-8601 datetime)
- `returnDateTime` (optional, ISO-8601 datetime)
- If `startDate` / `endDate` are date-only, the backend normalizes using Colombo-local date boundaries.
- Midnight `returnDateTime` values are treated as placeholders when date-only `endDate` is also provided.
- Vehicle must be available and have no overlapping active booking for requested range

## Success Response (200)
```json
{
  "id": "b-1",
  "userId": "u-1",
  "bookingId": "b-1",
  "vehicleId": "veh-123",
  "user": {
    "id": "u-1",
    "name": "Customer One",
    "email": "customer@example.com",
    "username": "customer@example.com"
  },
  "vehicle": {
    "id": "veh-123",
    "name": "Toyota Prius",
    "thumbnailUrl": "https://example.com/vehicle.jpg",
    "vehicleTypeId": "type-1",
    "rentalPrice": 15000,
    "availabilityStatus": "AVAILABLE",
    "make": "Toyota",
    "model": "Prius",
    "year": 2022,
    "licensePlate": "ABC-1234",
    "type": "HYBRID",
    "description": "Fuel efficient",
    "rentalPricePerDay": 15000,
    "imageUrls": [],
    "available": true,
    "underMaintenance": false,
    "adminHeld": false
  },
  "vehicleName": "Toyota Prius",
  "bookingTime": "2026-03-28T15:34:10.0820371",
  "createdAt": "2026-03-28T15:34:10.0820371",
  "startDate": "2026-04-10T00:00:00",
  "pickupDateTime": "2026-04-10T00:00:00",
  "endDate": "2026-04-13T00:00:00",
  "returnDateTime": "2026-04-13T00:00:00",
  "status": "PENDING",
  "advanceAmount": null,
  "totalPrice": 45000,
  "totalAmount": 45000,
  "advancePaid": false,
  "rejectionReason": null,
  "nicFrontDocumentId": "doc-nic",
  "drivingLicenseDocumentId": "doc-license"
}
```

> Note: `endDate` in response is stored as an exclusive boundary (`next day at 00:00:00`) to support overlap-safe range checks.

## Error Response (400, Validation)
```json
{
  "timestamp": "2026-03-28T10:22:11.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/bookings",
  "errors": [
    {
      "field": "endDate",
      "rejectedValue": "2026-04-10",
      "message": "endDate must be on or after startDate"
    }
  ]
}
```

## Common 400 Cases
- Missing `vehicleId`, `startDate`, or `endDate`
- Invalid date format (must be `yyyy-MM-dd` or ISO-8601 datetime)
- `startDate` or `endDate` in the past
- `endDate` before `startDate`
- Overlapping booking range for vehicle

