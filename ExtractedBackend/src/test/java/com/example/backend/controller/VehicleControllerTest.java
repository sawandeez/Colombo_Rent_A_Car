package com.example.backend.controller;

import com.example.backend.dto.VehicleTypeDto;
import com.example.backend.service.VehicleTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleControllerTest {

    private VehicleTypeService vehicleTypeService;
    private VehicleController controller;

    // stubbed vehicle service for filtering
    private static class StubVehicleService extends com.example.backend.service.VehicleService {
        private List<com.example.backend.dto.VehicleSummaryDto> returnList;
        private boolean throwOnCall;

        StubVehicleService() {
            super(null, null);
            returnList = List.of();
        }

        void setReturnList(List<com.example.backend.dto.VehicleSummaryDto> list) {
            this.returnList = list;
        }
        void setThrowOnCall(boolean val) { this.throwOnCall = val; }

        @Override
        public List<com.example.backend.dto.VehicleSummaryDto> getVehiclesByType(String typeId) {
            if (throwOnCall) throw new IllegalArgumentException("bad");
            return returnList;
        }
    }

    @BeforeEach
    void setup() {
        vehicleTypeService = new VehicleTypeService(null) {
            @Override
            public List<VehicleTypeDto> getAllTypes() {
                return List.of(new VehicleTypeDto("1", "SUV"));
            }
        };
        // default controller without vehicle filter service (tests below will reassign)
        controller = new VehicleController(null, vehicleTypeService, null);
    }

    @Test
    void getAllVehicleTypesReturnsList() {
        var response = controller.getAllVehicleTypes();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1, response.getBody().size());
        assertEquals("SUV", response.getBody().get(0).getName());
    }

    @Test
    void getVehiclesFiltersByTypeId() {
        StubVehicleService vs = new StubVehicleService();
        vs.setReturnList(List.of(new com.example.backend.dto.VehicleSummaryDto("Car1", "thumb1")));
        controller = new VehicleController(null, vehicleTypeService, vs);
        var resp = controller.getAllVehicles("someId");
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertNotNull(resp.getBody());
    }

    @Test
    void getVehiclesHandlesInvalidType() {
        StubVehicleService vs = new StubVehicleService();
        vs.setThrowOnCall(true);
        controller = new VehicleController(null, vehicleTypeService, vs);
        var resp = controller.getAllVehicles("bad");
        assertEquals(400, resp.getStatusCode().value());
        assertTrue(resp.getBody().toString().contains("bad"));
    }

    @Test
    void getAllVehicleTypesHandlesException() {
        vehicleTypeService = new VehicleTypeService(null) {
            @Override
            public List<VehicleTypeDto> getAllTypes() {
                throw new RuntimeException("db error");
            }
        };
        StubVehicleService vs = new StubVehicleService();
        controller = new VehicleController(null, vehicleTypeService, vs);
        var response = controller.getAllVehicleTypes();
        assertEquals(500, response.getStatusCode().value());
    }
}
