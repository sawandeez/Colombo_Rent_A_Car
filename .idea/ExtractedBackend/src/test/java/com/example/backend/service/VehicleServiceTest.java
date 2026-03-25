package com.example.backend.service;

import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.dto.VehicleUpsertRequestDto;
import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleAvailabilityStatus;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.repository.VehicleTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, vehicleTypeRepository);
    }

    @Test
    void createVehicleMapsAllExtendedFields() {
        VehicleUpsertRequestDto request = new VehicleUpsertRequestDto();
        request.setName("Suzuki Wagon R Hybrid");
        request.setThumbnailUrl("https://example.com/thumb.jpg");
        request.setVehicleTypeId("vt-1");
        request.setRentalPrice(new BigDecimal("950.00"));
        request.setMake("Suzuki");
        request.setModel("Wagon R Hybrid");
        request.setYear(2023);
        request.setLicensePlate("CAB-1234");
        request.setType("Sedan");
        request.setDescription("Fuel-efficient hybrid sedan");
        request.setRentalPricePerDay(new BigDecimal("1000"));
        request.setImageUrls(List.of("https://example.com/1.jpg"));
        request.setIsAvailable(true);
        request.setIsUnderMaintenance(false);
        request.setIsAdminHeld(false);

        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle saved = invocation.getArgument(0);
            saved.setId("veh-1");
            return saved;
        });

        VehicleSummaryDto response = vehicleService.createVehicle(request);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        Vehicle saved = captor.getValue();

        assertEquals("Suzuki", saved.getMake());
        assertEquals("Wagon R Hybrid", saved.getModel());
        assertEquals(2023, saved.getYear());
        assertEquals("CAB-1234", saved.getLicensePlate());
        assertEquals("Sedan", saved.getType());
        assertEquals("Fuel-efficient hybrid sedan", saved.getDescription());
        assertEquals(new BigDecimal("1000"), saved.getRentalPricePerDay());
        assertEquals(List.of("https://example.com/1.jpg"), saved.getImageUrls());
        assertEquals(VehicleAvailabilityStatus.AVAILABLE, saved.getAvailabilityStatus());

        assertEquals("Suzuki", response.getMake());
        assertEquals("Wagon R Hybrid", response.getModel());
        assertEquals(2023, response.getYear());
        assertEquals("Sedan", response.getType());
        assertEquals("AVAILABLE", response.getAvailabilityStatus());
        assertTrue(response.isAvailable());
        assertFalse(response.isUnderMaintenance());
        assertFalse(response.isAdminHeld());
    }

    @Test
    void createVehicleUsesDefaultFlagsWhenMissing() {
        VehicleUpsertRequestDto request = new VehicleUpsertRequestDto();
        request.setName("Default Flag Car");

        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleSummaryDto response = vehicleService.createVehicle(request);

        assertTrue(response.isAvailable());
        assertFalse(response.isUnderMaintenance());
        assertFalse(response.isAdminHeld());
        assertEquals("AVAILABLE", response.getAvailabilityStatus());
    }
}
