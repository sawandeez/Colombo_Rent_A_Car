package com.example.backend.service;

import com.example.backend.dto.VehicleSummaryDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleServiceTest {

    @Test
    void vehicleSummaryDtoWorks() {
        VehicleSummaryDto dto = new VehicleSummaryDto("Car A", "thumb");
        assertEquals("Car A", dto.getName());
        assertEquals("thumb", dto.getThumbnailUrl());
    }

    @Test
    void testDtoCreation() {
        VehicleSummaryDto dto = new VehicleSummaryDto();
        dto.setName("Vehicle");
        dto.setThumbnailUrl("url");
        assertNotNull(dto);
        assertEquals("Vehicle", dto.getName());
    }
}
