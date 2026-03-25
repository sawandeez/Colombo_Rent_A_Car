package com.example.backend.controller;

import com.example.backend.model.Booking;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.model.Vehicle;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingDocumentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService bookingService;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private VehicleRepository vehicleRepository;

    @MockBean
    private UserRepository userRepository;

    private Path uploadDir;

    @BeforeEach
    void setUp() throws IOException {
        uploadDir = Files.createTempDirectory("booking-doc-it");
        ReflectionTestUtils.setField(bookingService, "uploadBaseDir", uploadDir.toString());
        ReflectionTestUtils.setField(bookingService, "maxFileSizeBytes", 5 * 1024 * 1024L);

        Vehicle vehicle = new Vehicle();
        vehicle.setId("veh-1");
        vehicle.setAvailable(true);
        vehicle.setUnderMaintenance(false);
        vehicle.setAdminHeld(false);

        when(vehicleRepository.findById("veh-1")).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findOverlappingBookings(eq("veh-1"), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getId() == null) {
                booking.setId("booking-1");
            }
            return booking;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void firstBookingRequiresUploadsAndSucceedsWhenProvided() throws Exception {
        User user = buildUser("u-1", "customer@example.com", UserRole.CUSTOMER, null, null);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile bookingJson = bookingPart(nowPlusDays(1), nowPlusDays(3));
        MockMultipartFile nicFront = new MockMultipartFile("nicFront", "nic.jpg", "image/jpeg", "nic".getBytes());
        MockMultipartFile drivingLicense = new MockMultipartFile("drivingLicense", "license.png", "image/png", "license".getBytes());

        mockMvc.perform(multipart("/api/v1/bookings")
                        .file(bookingJson)
                        .file(nicFront)
                        .file(drivingLicense))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("u-1"))
                .andExpect(jsonPath("$.nicFrontPath").exists())
                .andExpect(jsonPath("$.drivingLicensePath").exists());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void secondBookingSucceedsWithoutReuploadWhenDocsAlreadyStored() throws Exception {
        User user = buildUser(
                "u-1",
                "customer@example.com",
                UserRole.CUSTOMER,
                "user-docs/u-1/nic-front-existing.jpg",
                "user-docs/u-1/driving-license-existing.png");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile bookingJson = bookingPart(nowPlusDays(2), nowPlusDays(4));

        mockMvc.perform(multipart("/api/v1/bookings")
                        .file(bookingJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nicFrontPath").value("user-docs/u-1/nic-front-existing.jpg"))
                .andExpect(jsonPath("$.drivingLicensePath").value("user-docs/u-1/driving-license-existing.png"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void missingSingleRequiredDocumentReturns400() throws Exception {
        User user = buildUser("u-1", "customer@example.com", UserRole.CUSTOMER, "user-docs/u-1/nic.jpg", null);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile bookingJson = bookingPart(nowPlusDays(1), nowPlusDays(2));

        mockMvc.perform(multipart("/api/v1/bookings")
                        .file(bookingJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("drivingLicense is required")));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void invalidFileTypeReturns400() throws Exception {
        User user = buildUser("u-1", "customer@example.com", UserRole.CUSTOMER, null, null);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile bookingJson = bookingPart(nowPlusDays(1), nowPlusDays(2));
        MockMultipartFile nicFront = new MockMultipartFile("nicFront", "nic.pdf", "application/pdf", "bad".getBytes());
        MockMultipartFile drivingLicense = new MockMultipartFile("drivingLicense", "license.png", "image/png", "ok".getBytes());

        mockMvc.perform(multipart("/api/v1/bookings")
                        .file(bookingJson)
                        .file(nicFront)
                        .file(drivingLicense))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("image/jpeg or image/png")));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void tooLargeFileReturns400() throws Exception {
        User user = buildUser("u-1", "customer@example.com", UserRole.CUSTOMER, null, null);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile bookingJson = bookingPart(nowPlusDays(1), nowPlusDays(2));
        byte[] tooLarge = new byte[(5 * 1024 * 1024) + 1];
        MockMultipartFile nicFront = new MockMultipartFile("nicFront", "nic.jpg", "image/jpeg", tooLarge);
        MockMultipartFile drivingLicense = new MockMultipartFile("drivingLicense", "license.png", "image/png", "ok".getBytes());

        mockMvc.perform(multipart("/api/v1/bookings")
                        .file(bookingJson)
                        .file(nicFront)
                        .file(drivingLicense))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("5MB size limit")));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "CUSTOMER")
    void ownerCanDownloadOwnDocument() throws Exception {
        User owner = buildUser("u-owner", "owner@example.com", UserRole.CUSTOMER,
                "user-docs/u-owner/nic-front-file.jpg",
                "user-docs/u-owner/driving-license-file.png");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        Path ownerDir = uploadDir.resolve("user-docs").resolve("u-owner");
        Files.createDirectories(ownerDir);
        Files.writeString(ownerDir.resolve("nic-front-file.jpg"), "owner-nic");

        mockMvc.perform(get("/api/users/me/documents/nic-front"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user2@example.com", roles = "CUSTOMER")
    void nonAdminCannotDownloadAnotherUsersDocument() throws Exception {
        mockMvc.perform(get("/api/admin/users/u-owner/documents/nic-front"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanDownloadAnyUsersDocument() throws Exception {
        User admin = buildUser("u-admin", "admin@example.com", UserRole.ADMIN, null, null);
        User target = buildUser("u-target", "target@example.com", UserRole.CUSTOMER,
                "user-docs/u-target/nic-front-file.jpg",
                "user-docs/u-target/driving-license-file.png");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById("u-target")).thenReturn(Optional.of(target));

        Path targetDir = uploadDir.resolve("user-docs").resolve("u-target");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("nic-front-file.jpg"), "target-nic");

        mockMvc.perform(get("/api/admin/users/u-target/documents/nic-front"))
                .andExpect(status().isOk());
    }

    private MockMultipartFile bookingPart(LocalDateTime pickupDate, LocalDateTime returnDate) {
        String payload = "{" +
                "\"vehicleId\":\"veh-1\"," +
                "\"pickupDate\":\"" + pickupDate + "\"," +
                "\"returnDate\":\"" + returnDate + "\"" +
                "}";
        return new MockMultipartFile("booking", "booking.json", "application/json", payload.getBytes());
    }

    private LocalDateTime nowPlusDays(int days) {
        return LocalDateTime.now().plusDays(days).withNano(0);
    }

    private User buildUser(String id, String email, UserRole role, String nicPath, String dlPath) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setNicFrontPath(nicPath);
        user.setDrivingLicensePath(dlPath);
        return user;
    }
}

