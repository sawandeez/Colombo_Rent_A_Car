package com.example.backend.config;

import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleType;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final VehicleTypeRepository typeRepo;
    private final VehicleRepository vehicleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            try {
                seedAdminUsers();
                seedVehicleTypes();
                seedVehicles();
                backfillAvailabilityStatus();
            } catch (Exception e) {
                log.warn("Failed to seed data (MongoDB may not be available): {}", e.getMessage());
            }
        };
    }

    private void seedAdminUsers() {
        try {
            if (userRepo.existsByEmail("admin@example.com")) {
                log.info("Admin user already exists — skipping user seed.");
                return;
            }

            User adminUser = new User();
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setName("Admin User");
            adminUser.setPhone("0112345678");
            adminUser.setDistrict("Colombo");
            adminUser.setCity("Colombo");
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setDocumentsVerified(true);

            userRepo.save(adminUser);
            log.info("SEEDER - Admin user created: admin@example.com");
        } catch (Exception e) {
            log.warn("Failed to seed admin users: {}", e.getMessage());
        }
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

            List<VehicleType> types = typeRepo.findAll();
            String suvId = types.stream().filter(t -> "SUV".equals(t.getName())).findFirst().map(VehicleType::getId).orElse(null);
            String sedanId = types.stream().filter(t -> "Sedan".equals(t.getName())).findFirst().map(VehicleType::getId).orElse(null);
            String vanId = types.stream().filter(t -> "Van".equals(t.getName())).findFirst().map(VehicleType::getId).orElse(null);

            Vehicle v1 = new Vehicle();
            v1.setName("Toyota RAV4");
            v1.setThumbnailUrl("https://example.com/rav4.jpg");
            v1.setVehicleTypeId(suvId);
            v1.setRentalPrice(new BigDecimal("8000"));
            v1.setMake("Toyota");
            v1.setModel("RAV4");
            v1.setYear(2023);
            v1.setType("SUV");
            v1.setDescription("A reliable SUV for family trips.");
            v1.setRentalPricePerDay(new BigDecimal("8000"));
            v1.setImageUrls(List.of("https://example.com/rav4.jpg"));
            v1.setAvailable(true);

            Vehicle v2 = new Vehicle();
            v2.setName("Honda Civic");
            v2.setThumbnailUrl("https://example.com/civic.jpg");
            v2.setVehicleTypeId(sedanId);
            v2.setRentalPrice(new BigDecimal("5000"));
            v2.setMake("Honda");
            v2.setModel("Civic");
            v2.setYear(2022);
            v2.setType("Sedan");
            v2.setDescription("Comfortable sedan for city driving.");
            v2.setRentalPricePerDay(new BigDecimal("5000"));
            v2.setImageUrls(List.of("https://example.com/civic.jpg"));
            v2.setAvailable(true);

            Vehicle v3 = new Vehicle();
            v3.setName("Ford Transit");
            v3.setThumbnailUrl("https://example.com/transit.jpg");
            v3.setVehicleTypeId(vanId);
            v3.setRentalPrice(new BigDecimal("9000"));
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

    private void backfillAvailabilityStatus() {
        List<Vehicle> vehiclesToBackfill = vehicleRepo.findByAvailabilityStatusIsNull();
        if (vehiclesToBackfill.isEmpty()) {
            log.info("No vehicles required availabilityStatus backfill.");
            return;
        }

        vehiclesToBackfill.forEach(Vehicle::syncStatusFromFlags);
        vehicleRepo.saveAll(vehiclesToBackfill);
        log.info("Backfilled availabilityStatus for {} vehicles.", vehiclesToBackfill.size());
    }
}
