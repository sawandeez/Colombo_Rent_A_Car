package com.example.backend.controller;

import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.dto.VehicleUpsertRequestDto;
import jakarta.validation.Valid;
import com.example.backend.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VehicleSummaryDto>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping(value = "/available", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VehicleSummaryDto>> getAvailableVehicles() {
        return ResponseEntity.ok(vehicleService.getAllAvailableVehicles());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VehicleSummaryDto> getVehicle(@PathVariable String id) {
        return ResponseEntity.ok(vehicleService.getVehicle(id));
    }

    @GetMapping(value = "/type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VehicleSummaryDto>> getVehiclesByType(@PathVariable String type) {
        return ResponseEntity.ok(vehicleService.getAvailableVehiclesByType(type));
    }

    @PostMapping
    public ResponseEntity<VehicleSummaryDto> createVehicle(@Valid @RequestBody VehicleUpsertRequestDto dto) {
        return ResponseEntity.ok(vehicleService.createVehicle(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleSummaryDto> updateVehicle(@PathVariable String id, @Valid @RequestBody VehicleUpsertRequestDto dto) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable String id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/hold")
    public ResponseEntity<Void> holdVehicle(@PathVariable String id) {
        vehicleService.setAdminHold(id, true);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<Void> resumeVehicle(@PathVariable String id) {
        vehicleService.setAdminHold(id, false);
        return ResponseEntity.ok().build();
    }
}
