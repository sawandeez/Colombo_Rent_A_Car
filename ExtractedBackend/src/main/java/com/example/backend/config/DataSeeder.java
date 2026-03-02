package com.example.backend.config;

import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleType;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final VehicleTypeRepository typeRepo;
    private final VehicleRepository vehicleRepo;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            seedVehicleTypes();
            seedVehicles();
        };
    }

    private void seedVehicleTypes() {
        if (typeRepo.count() > 0) {
            log.info("Vehicle types already exist — skipping type seed.");
            return;
        }

        VehicleType suv = new VehicleType("SUV");
        VehicleType sedan = new VehicleType("Sedan");
        VehicleType van = new VehicleType("Van");

        typeRepo.saveAll(List.of(suv, sedan, van));

        log.info("Seeded vehicle types: SUV, Sedan, Van");
    }

    private void seedVehicles() {
        if (vehicleRepo.count() > 0) {
            log.info("Vehicles already exist — skipping vehicle seed.");
            return;
        }

        List<VehicleType> types = typeRepo.findAll();
        String suvId = types.stream().filter(t -> "SUV".equals(t.getName())).findFirst().map(VehicleType::getId).orElse(null);
        String sedanId = types.stream().filter(t -> "Sedan".equals(t.getName())).findFirst().map(VehicleType::getId).orElse(null);
        String vanId = types.stream().filter(t -> "Van".equals(t.getName())).findFirst().map(VehicleType::getId).orElse(null);

        Vehicle v1 = new Vehicle();
        v1.setName("Toyota RAV4");
        v1.setThumbnailUrl("https://example.com/rav4.jpg");
        v1.setVehicleTypeId(suvId);
        v1.setRentalPrice(new BigDecimal("8000"));
        v1.setAvailabilityStatus("AVAILABLE");

        Vehicle v2 = new Vehicle();
        v2.setName("Honda Civic");
        v2.setThumbnailUrl("https://example.com/civic.jpg");
        v2.setVehicleTypeId(sedanId);
        v2.setRentalPrice(new BigDecimal("5000"));
        v2.setAvailabilityStatus("AVAILABLE");

        Vehicle v3 = new Vehicle();
        v3.setName("Ford Transit");
        v3.setThumbnailUrl("https://example.com/transit.jpg");
        v3.setVehicleTypeId(vanId);
        v3.setRentalPrice(new BigDecimal("9000"));
        v3.setAvailabilityStatus("UNAVAILABLE");

        vehicleRepo.saveAll(List.of(v1, v2, v3));

        log.info("Seeded 3 vehicles linked to types");
    }
}
