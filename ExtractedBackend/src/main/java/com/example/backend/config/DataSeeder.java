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

    // Disabled for now - MongoDB may not always be available
    // Enable this bean when you have MongoDB running
    // @Bean
    public CommandLineRunner seedData() {
        return args -> {
            try {
                seedVehicleTypes();
                seedVehicles();
            } catch (Exception e) {
                log.warn("Failed to seed data (MongoDB may not be available): {}", e.getMessage());
                log.info("Application will continue without seeded data. You can add data manually via API.");
            }
        };
    }

    private void seedVehicleTypes() {
        try {
            if (typeRepo.count() > 0) {
                log.info("Vehicle types already exist — skipping type seed.");
                return;
            }

            VehicleType suv = new VehicleType("SUV");
            VehicleType sedan = new VehicleType("Sedan");
            VehicleType van = new VehicleType("Van");

            typeRepo.saveAll(List.of(suv, sedan, van));

            log.info("Seeded vehicle types: SUV, Sedan, Van");
        } catch (Exception e) {
            log.warn("Failed to seed vehicle types: {}", e.getMessage());
        }
    }

    private void seedVehicles() {
        try {
            if (vehicleRepo.count() > 0) {
                log.info("Vehicles already exist — skipping vehicle seed.");
                return;
            }

            Vehicle v1 = new Vehicle();
            v1.setMake("Toyota");
            v1.setModel("RAV4");
            v1.setYear(2023);
            v1.setType("SUV");
            v1.setDescription("A reliable SUV for family trips.");
            v1.setRentalPricePerDay(new BigDecimal("8000"));
            v1.setImageUrls(List.of("https://example.com/rav4.jpg"));
            v1.setAvailable(true);

            Vehicle v2 = new Vehicle();
            v2.setMake("Honda");
            v2.setModel("Civic");
            v2.setYear(2022);
            v2.setType("Sedan");
            v2.setDescription("Comfortable sedan for city driving.");
            v2.setRentalPricePerDay(new BigDecimal("5000"));
            v2.setImageUrls(List.of("https://example.com/civic.jpg"));
            v2.setAvailable(true);

            Vehicle v3 = new Vehicle();
            v3.setMake("Ford");
            v3.setModel("Transit");
            v3.setYear(2021);
            v3.setType("Van");
            v3.setDescription("Spacious van for group travel.");
            v3.setRentalPricePerDay(new BigDecimal("9000"));
            v3.setImageUrls(List.of("https://example.com/transit.jpg"));
            v3.setAvailable(false);

            vehicleRepo.saveAll(List.of(v1, v2, v3));

            log.info("Seeded vehicles: Toyota RAV4, Honda Civic, Ford Transit");
        } catch (Exception e) {
            log.warn("Failed to seed vehicles: {}", e.getMessage());
        }
    }
}
