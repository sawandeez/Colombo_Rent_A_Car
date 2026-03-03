package com.example.backend.service;

import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.model.Vehicle;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.repository.VehicleTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          VehicleTypeRepository vehicleTypeRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    public List<VehicleSummaryDto> getVehiclesByType(String typeId) {
        logger.debug("Fetching vehicles for typeId={}", typeId);

        if (typeId == null || typeId.isEmpty()) {
            throw new IllegalArgumentException("typeId must be provided");
        }

        boolean exists = vehicleTypeRepository.existsById(typeId);
        if (!exists) {
            logger.warn("Requested vehicle type does not exist: {}", typeId);
            throw new IllegalArgumentException("Invalid vehicle type id");
        }

        List<Vehicle> list = vehicleRepository.findByVehicleTypeId(typeId);
        return list.stream()
                .map(v -> new VehicleSummaryDto(v.getName(), v.getThumbnailUrl()))
                .collect(Collectors.toList());
    }
}
