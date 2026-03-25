package com.example.backend.controller;

import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleControllerTest {

    private VehicleController controller;
    private VehicleService vehicleService;

    private static class StubVehicleService extends VehicleService {
        private List<VehicleSummaryDto> returnList;
        private boolean throwOnCall;

        StubVehicleService() {
            super(null, null);
            returnList = List.of();
        }

        void setReturnList(List<VehicleSummaryDto> list) {
            this.returnList = list;
        }

        void setThrowOnCall(boolean val) {
            this.throwOnCall = val;
        }

        @Override
        public List<VehicleSummaryDto> getAllVehicles() {
            if (throwOnCall) throw new IllegalArgumentException("bad");
            return returnList;
        }
    }

    @BeforeEach
    void setup() {
        vehicleService = new StubVehicleService();
        controller = new VehicleController(vehicleService);
    }

    @Test
    void getAllVehiclesReturnsList() {
        StubVehicleService vs = (StubVehicleService) vehicleService;
        vs.setReturnList(List.of(new VehicleSummaryDto("Car1", "thumb1")));

        var response = controller.getAllVehicles();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1, response.getBody().size());
        assertEquals("Car1", response.getBody().get(0).getName());
    }

    @Test
    void getAllVehiclesHandlesException() {
        StubVehicleService vs = (StubVehicleService) vehicleService;
        vs.setThrowOnCall(true);

        assertThrows(IllegalArgumentException.class, () -> controller.getAllVehicles());
    }
}
