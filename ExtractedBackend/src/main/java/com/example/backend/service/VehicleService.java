package com.example.backend.service;

import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.dto.VehicleUpsertRequestDto;
import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleAvailabilityStatus;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.repository.VehicleTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
    public List<VehicleSummaryDto> getVehiclesByType(String typeId) {
        logger.debug("Fetching vehicles for typeId={}", typeId);

        if (typeId == null || typeId.isBlank()) {
            throw new IllegalArgumentException("typeId must be provided");
        }

        boolean exists = vehicleTypeRepository.existsById(typeId);
        if (!exists) {
            logger.warn("Requested vehicle type does not exist: {}", typeId);
            throw new IllegalArgumentException("Invalid vehicle type id");
        }

        List<Vehicle> list = vehicleRepository.findByVehicleTypeId(typeId);
        return list.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<VehicleSummaryDto> getAllAvailableVehicles() {
        logger.debug("Fetching all available vehicles");
        List<Vehicle> vehicles = vehicleRepository.findByAvailabilityStatus(VehicleAvailabilityStatus.AVAILABLE);
        return vehicles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<VehicleSummaryDto> getAllVehicles() {
        logger.debug("Fetching all vehicles");
        return vehicleRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<VehicleSummaryDto> getAvailableVehiclesByType(String type) {
        logger.debug("Fetching available vehicles for type={}", type);
        List<Vehicle> vehicles = vehicleRepository.findByTypeAndAvailabilityStatus(type, VehicleAvailabilityStatus.AVAILABLE);
        return vehicles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public VehicleSummaryDto getVehicle(String id) {
        logger.debug("Fetching vehicle with id={}", id);
        Optional<Vehicle> vehicle = vehicleRepository.findById(id);
        if (vehicle.isEmpty()) {
            throw new IllegalArgumentException("Vehicle not found with id: " + id);
        }
        return convertToDto(vehicle.get());
    }

    public VehicleSummaryDto createVehicle(VehicleUpsertRequestDto dto) {
        logger.debug("Creating new vehicle: {}", dto.getName());
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Vehicle name is required");
        }
        Vehicle vehicle = new Vehicle();
        applyUpsertToVehicle(vehicle, dto, true);

        Vehicle saved = vehicleRepository.save(vehicle);
        return convertToDto(saved);
    }

    public VehicleSummaryDto updateVehicle(String id, VehicleUpsertRequestDto dto) {
        logger.debug("Updating vehicle with id={}", id);
        Optional<Vehicle> existing = vehicleRepository.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Vehicle not found with id: " + id);
        }

        Vehicle vehicle = existing.get();
        applyUpsertToVehicle(vehicle, dto, false);

        Vehicle updated = vehicleRepository.save(vehicle);
        return convertToDto(updated);
    }

    public void deleteVehicle(String id) {
        logger.debug("Deleting vehicle with id={}", id);
        if (!vehicleRepository.existsById(id)) {
            throw new IllegalArgumentException("Vehicle not found with id: " + id);
        }
        vehicleRepository.deleteById(id);
    }

    public void setAdminHold(String id, boolean onHold) {
        logger.debug("Setting admin hold for vehicle id={} to {}", id, onHold);
        Optional<Vehicle> existing = vehicleRepository.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Vehicle not found with id: " + id);
        }

        Vehicle vehicle = existing.get();
        vehicle.setAdminHeld(onHold);
        vehicleRepository.save(vehicle);
    }

    private VehicleSummaryDto convertToDto(Vehicle vehicle) {
        VehicleSummaryDto dto = new VehicleSummaryDto();
        dto.setId(vehicle.getId());
        dto.setName(vehicle.getName());
        dto.setThumbnailUrl(vehicle.getThumbnailUrl());
        dto.setVehicleTypeId(vehicle.getVehicleTypeId());
        dto.setRentalPrice(vehicle.getRentalPrice());
        VehicleAvailabilityStatus resolvedStatus = vehicle.getAvailabilityStatus() != null
                ? vehicle.getAvailabilityStatus()
                : VehicleAvailabilityStatus.fromFlags(vehicle.isAvailable(), vehicle.isUnderMaintenance(), vehicle.isAdminHeld());
        dto.setAvailabilityStatus(resolvedStatus.name());
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setType(vehicle.getType());
        dto.setDescription(vehicle.getDescription());
        dto.setRentalPricePerDay(vehicle.getRentalPricePerDay());
        dto.setImageUrls(vehicle.getImageUrls());
        dto.setAvailable(vehicle.isAvailable());
        dto.setUnderMaintenance(vehicle.isUnderMaintenance());
        dto.setAdminHeld(vehicle.isAdminHeld());
        return dto;
    }

    private void applyUpsertToVehicle(Vehicle vehicle, VehicleUpsertRequestDto dto, boolean isCreate) {
        if (isCreate || dto.getName() != null) vehicle.setName(dto.getName());
        if (isCreate || dto.getThumbnailUrl() != null) vehicle.setThumbnailUrl(dto.getThumbnailUrl());
        if (isCreate || dto.getVehicleTypeId() != null) vehicle.setVehicleTypeId(dto.getVehicleTypeId());
        if (isCreate || dto.getRentalPrice() != null) vehicle.setRentalPrice(dto.getRentalPrice());

        if (isCreate || dto.getMake() != null) vehicle.setMake(dto.getMake());
        if (isCreate || dto.getModel() != null) vehicle.setModel(dto.getModel());
        if (isCreate || dto.getYear() != null) vehicle.setYear(dto.getYear() == null ? 0 : dto.getYear());
        if (isCreate || dto.getLicensePlate() != null) vehicle.setLicensePlate(dto.getLicensePlate());
        if (isCreate || dto.getType() != null) vehicle.setType(dto.getType());
        if (isCreate || dto.getDescription() != null) vehicle.setDescription(dto.getDescription());
        if (isCreate || dto.getRentalPricePerDay() != null) vehicle.setRentalPricePerDay(dto.getRentalPricePerDay());
        if (isCreate || dto.getImageUrls() != null) vehicle.setImageUrls(dto.getImageUrls());

        if (isCreate) {
            vehicle.setAvailable(dto.getIsAvailable() == null || dto.getIsAvailable());
            vehicle.setUnderMaintenance(dto.getIsUnderMaintenance() != null && dto.getIsUnderMaintenance());
            vehicle.setAdminHeld(dto.getIsAdminHeld() != null && dto.getIsAdminHeld());
        } else {
            if (dto.getIsAvailable() != null) vehicle.setAvailable(dto.getIsAvailable());
            if (dto.getIsUnderMaintenance() != null) vehicle.setUnderMaintenance(dto.getIsUnderMaintenance());
            if (dto.getIsAdminHeld() != null) vehicle.setAdminHeld(dto.getIsAdminHeld());
        }

        // Ignore request availabilityStatus and derive from current flags.
        vehicle.syncStatusFromFlags();
    }
}
