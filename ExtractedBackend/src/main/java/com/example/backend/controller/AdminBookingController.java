package com.carrental.system.controller;

import com.carrental.system.dto.BookingResponse;
import com.carrental.system.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping("/requests")
    public ResponseEntity<List<BookingResponse>> getAllRequests() {
        return ResponseEntity.ok(bookingService.getAllBookingRequests());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveBooking(@PathVariable String id) {
        bookingService.approveBooking(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectBooking(@PathVariable String id, @RequestBody String reason) {
        bookingService.rejectBooking(id, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/set-advance")
    public ResponseEntity<Void> setAdvance(@PathVariable String id, @RequestBody BigDecimal amount) {
        bookingService.setAdvanceAmount(id, amount);
        return ResponseEntity.ok().build();
    }
}
