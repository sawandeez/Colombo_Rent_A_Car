package com.example.backend.service;

import com.example.backend.dto.VehicleTypeDto;
import com.example.backend.model.VehicleType;
import com.example.backend.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;

    public List<VehicleTypeDto> getAllTypes() {
        List<VehicleType> types = vehicleTypeRepository.findAll();
        if (CollectionUtils.isEmpty(types)) {
            return List.of();
        }
        return types.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private VehicleTypeDto mapToDto(VehicleType type) {
        return new VehicleTypeDto(type.getId(), type.getName());
    }
}
