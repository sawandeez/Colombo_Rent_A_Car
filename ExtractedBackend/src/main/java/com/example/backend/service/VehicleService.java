package com.example.backend.service;

import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleType;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.repository.VehicleTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * Returns vehicles whose {@code type} name matches the given {@code typeId}.
     * <p>
     * The frontend passes the MongoDB ObjectId of a VehicleType document.
     * We resolve that id to the type's name first, then use the name to
     * query the vehicles collection — which stores the type as a plain string.
     * </p>
     *
     * @param typeId the MongoDB ObjectId of the requested VehicleType
     * @throws IllegalArgumentException if typeId is blank or no matching type is
     *                                  found
     */
    public List<Vehicle> getVehiclesByType(String typeId) {
        logger.debug("Fetching vehicles for typeId={}", typeId);

        if (typeId == null || typeId.isBlank()) {
            throw new IllegalArgumentException("typeId must be provided");
        }

        // Resolve the ObjectId → VehicleType document → type name
        VehicleType vehicleType = vehicleTypeRepository.findById(typeId)
                .orElseThrow(() -> {
                    logger.warn("No VehicleType found for id={}", typeId);
                    return new IllegalArgumentException("No vehicle type found for id: " + typeId);
                });

        String typeName = vehicleType.getName();
        logger.debug("Resolved typeId={} to typeName={}", typeId, typeName);

        return vehicleRepository.findByType(typeName);
    }
}
