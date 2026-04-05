# Payment API Contract (PayHere + Mock)

## 1) Initiate booking payment

- **Method**: `POST`
- **Endpoints**:
  - `/api/v1/bookings/{bookingId}/payments/initiate`
  - `/api/bookings/{bookingId}/payments/initiate` (compat alias)
- **Auth**: `ROLE_CUSTOMER` or `ROLE_ADMIN`
- **Behavior**:
  - Uses configured gateway from `app.payment.gateway`
  - If `PAYHERE` is configured but merchant credentials are missing, backend auto-falls back to `MOCK`
  - Always creates `payment_transactions` record with `status=INITIATED`

### Response `200 OK` (PayHere mode)

```json
{
  "bookingId": "b-123",
  "gateway": "PAYHERE",
  "orderId": "ORD-b-123-AB12CD34",
  "payhereUrl": "https://sandbox.payhere.lk/pay/checkout",
  "redirectUrl": null,
  "fields": {
    "merchant_id": "1211144",
    "return_url": "https://frontend.example.com/payment/success?bookingId=b-123",
    "cancel_url": "https://frontend.example.com/payment/fail?bookingId=b-123",
    "notify_url": "https://backend.example.com/api/v1/payments/payhere/notify",
    "order_id": "ORD-b-123-AB12CD34",
    "items": "Advance payment for booking b-123",
    "amount": "15000.00",
    "currency": "LKR",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john@example.com",
    "phone": "+94770000000",
    "address": "No 1, Main Street",
    "city": "Colombo",
    "country": "Sri Lanka",
    "hash": "GENERATED_HASH"
  }
}
```

### Response `200 OK` (Mock mode)

```json
{
  "bookingId": "b-123",
  "gateway": "MOCK",
  "orderId": "ORD-b-123-AB12CD34",
  "payhereUrl": null,
  "redirectUrl": "http://localhost:8080/api/v1/payments/mock/checkout?orderId=ORD-b-123-AB12CD34",
  "fields": null
}
```

### Error `400/404`

```json
{
  "timestamp": "2026-04-05T08:20:02.020Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Booking is not payable in its current state",
  "path": "/api/v1/bookings/b-123/payments/initiate"
}
```

---

## 2) Mock checkout + completion (demo flow)

### GET `/api/v1/payments/mock/checkout?orderId=...`

- Returns a simple HTML page with two actions:
  - Simulate Success
  - Simulate Failure

### POST `/api/v1/payments/mock/complete`

- **Auth**: none (demo endpoint)
- **Request**:

```json
{
  "orderId": "ORD-b-123-AB12CD34",
  "status": "SUCCESS"
}
```

- **Response `200 OK`**:

```json
{
  "status": "ok",
  "orderId": "ORD-b-123-AB12CD34",
  "gateway": "MOCK",
  "paymentStatus": "SUCCESS",
  "redirectUrl": "http://localhost:5173/payment/success?bookingId=b-123"
}
```

- Completion effects mirror PayHere webhook:
  - Transaction status -> `SUCCESS` or `FAILED`
  - Booking `paymentStatus` -> `SUCCESS` or `FAILED`
  - On success: `paymentDate=now`, `advancePaid=true`, booking `status=CONFIRMED`

---

## 3) PayHere webhook notify

- **Method**: `POST`
- **Endpoints**:
  - `/api/v1/payments/payhere/notify`
  - `/api/payments/payhere/notify` (compat alias)
- **Auth**: none
- **Content-Type**: `application/x-www-form-urlencoded`
- **Signature check**:
  - Verified when merchant credentials are configured
  - Skipped with warning when credentials are missing (useful in demo mode)

### Response `200 OK`

```json
{
  "status": "ok"
}
```

### Webhook business effects

- Update `payment_transactions` by `order_id`
- Save gateway transaction id (`payment_id`)
- Set transaction status to `SUCCESS` when status code is `2`; otherwise `FAILED`
- Update booking:
  - `paymentStatus = SUCCESS` + `paymentDate = now()` + `status = CONFIRMED` on success
  - `paymentStatus = FAILED` on failure
- Idempotent behavior: repeated notifications do not downgrade already successful payment

---

## 4) Booking response payment fields

`BookingResponse` includes:

- `paymentStatus`: `INITIATED | SUCCESS | FAILED`
- `paymentDate`: ISO-8601 timestamp or `null`

Example:

```json
{
  "id": "b-123",
  "status": "CONFIRMED",
  "paymentStatus": "SUCCESS",
  "paymentDate": "2026-04-05T08:22:17.185Z"
}
```
