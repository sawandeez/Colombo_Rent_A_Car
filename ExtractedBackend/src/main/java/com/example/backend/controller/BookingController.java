package com.example.backend.controller;

import com.example.backend.dto.BookingRequest;
import com.example.backend.dto.BookingCreateRequest;
import com.example.backend.dto.BookingResponse;
import com.example.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody @Valid BookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookingResponse> createBookingWithDocuments(
            @RequestPart("booking") @Valid BookingCreateRequest booking,
            @RequestPart(value = "nicFront", required = false) MultipartFile nicFront,
            @RequestPart(value = "drivingLicense", required = false) MultipartFile drivingLicense) {
        BookingResponse response = bookingService.createBookingWithDocuments(booking, nicFront, drivingLicense);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        return ResponseEntity.ok(bookingService.getMyBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable String id) {
        // Ownership check is enforced in BookingService.getBooking()
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable String id) {
        // Ownership check is enforced in BookingService.cancelBooking()
        bookingService.cancelBooking(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/vehicle/{vehicleId}/booked-dates")
    public ResponseEntity<List<String>> getBookedDates(@PathVariable String vehicleId) {
        return ResponseEntity.ok(bookingService.getBookedDatesForVehicle(vehicleId));
    }
}
