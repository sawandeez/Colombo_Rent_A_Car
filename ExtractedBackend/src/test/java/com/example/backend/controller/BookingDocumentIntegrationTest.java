package com.example.backend.controller;

import com.example.backend.dto.BookingCreateRequest;
import com.example.backend.dto.BookingResponse;
import com.example.backend.dto.UserDocumentMetadataResponse;
import com.example.backend.exception.ApiFieldError;
import com.example.backend.exception.RequestValidationException;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.DocumentCategory;
import com.example.backend.security.JwtService;
import com.example.backend.security.SecurityConfiguration;
import com.example.backend.service.BookingService;
import com.example.backend.service.UserDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        BookingController.class,
        AdminBookingController.class,
        UserDocumentController.class,
        AdminUserDocumentController.class
})
@Import(SecurityConfiguration.class)
class BookingDocumentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserDocumentService userDocumentService;

    /*
     * Do NOT mock JwtAuthenticationFilter itself — mocking it stubs doFilterInternal()
     * as a no-op, which breaks the servlet filter chain so requests never reach
     * DispatcherServlet or Spring Security.
     *
     * Instead, mock only its two dependencies so the REAL filter is created.
     * The real filter always calls filterChain.doFilter() when no Authorization header
     * is present (which is the case in all @WithMockUser tests), letting the chain
     * continue normally.  @WithMockUser supplies the SecurityContext directly.
     */
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void userUploadsMandatoryDocumentAndDownloadsIt() throws Exception {
        UserDocumentMetadataResponse uploaded = UserDocumentMetadataResponse.builder()
                .id("doc-1")
                .ownerUserId("user-1")
                .category(DocumentCategory.NIC_FRONT)
                .originalFilename("nic-front.pdf")
                .contentType("application/pdf")
                .size(100)
                .createdAt(LocalDateTime.now())
                .build();

        when(userDocumentService.uploadForCurrentUser(any(), any())).thenReturn(uploaded);
        when(userDocumentService.downloadCurrentUserDocument("doc-1"))
                .thenReturn(new UserDocumentService.DownloadDocumentResponse(
                        new ByteArrayResource("bytes".getBytes()),
                        "application/pdf",
                        "nic-front.pdf"));

        // category is a plain form-field — use @RequestParam (not @RequestPart) so
        // Spring's ConversionService converts "NIC_FRONT" → DocumentCategory.NIC_FRONT.
        mockMvc.perform(multipart("/api/v1/users/me/documents")
                        .file(new MockMultipartFile("file", "nic-front.pdf", "application/pdf", "bytes".getBytes()))
                        .param("category", "NIC_FRONT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("doc-1"))
                .andExpect(jsonPath("$.category").value("NIC_FRONT"));

        mockMvc.perform(get("/api/v1/users/me/documents/doc-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void userCannotDownloadAnotherUsersDocument() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/user-2/documents/doc-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingCreateFailsWith400WhenMandatoryDocumentsMissing() throws Exception {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId("veh-1");
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));

        when(bookingService.createBooking(any(BookingCreateRequest.class)))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Missing mandatory document: DRIVING_LICENSE"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing mandatory document: DRIVING_LICENSE"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingCreateSucceedsWhenMandatoryDocumentsExist() throws Exception {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId("veh-1");
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setPickupDateTime(LocalDateTime.now().plusDays(1).toString());
        request.setReturnDateTime(LocalDateTime.now().plusDays(2).toString());

        BookingResponse response = new BookingResponse();
        response.setId("b-1");
        response.setStatus(BookingStatus.PENDING);
        response.setNicFrontDocumentId("doc-nic");
        response.setDrivingLicenseDocumentId("doc-license");

        when(bookingService.createBooking(any(BookingCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b-1"))
                .andExpect(jsonPath("$.nicFrontDocumentId").value("doc-nic"))
                .andExpect(jsonPath("$.drivingLicenseDocumentId").value("doc-license"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingCreateFailsWith400WhenDateFormatIsInvalid() throws Exception {
        String payload = """
                {
                  "vehicleId": "veh-1",
                  "startDate": "2026/04/01",
                  "endDate": "2026-04-02"
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingCreateFailsWhenEndDateIsBeforeStartDate() throws Exception {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId("veh-1");
        request.setStartDate(LocalDate.now().plusDays(4));
        request.setEndDate(LocalDate.now().plusDays(2));

        when(bookingService.createBooking(any(BookingCreateRequest.class)))
                .thenThrow(new RequestValidationException(
                        "Validation failed",
                        new ApiFieldError("endDate", request.getEndDate(), "endDate must be after startDate")));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("endDate"))
                .andExpect(jsonPath("$.errors[0].message").value("endDate must be after startDate"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingCreateFailsWhenOverlappingBookingExists() throws Exception {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId("veh-1");
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(bookingService.createBooking(any(BookingCreateRequest.class)))
                .thenThrow(new RequestValidationException(
                        "Validation failed",
                        new ApiFieldError("startDate", request.getStartDate(), "Vehicle is already booked for the requested period")));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Vehicle is already booked for the requested period"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingCreateFailsWhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field=='vehicleId')].message").value(hasItem("Vehicle ID is required")))
                .andExpect(jsonPath("$.errors[?(@.field=='startDate')].message").value(hasItem("startDate is required")))
                .andExpect(jsonPath("$.errors[?(@.field=='endDate')].message").value(hasItem("endDate is required")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanListAndApproveAndRejectBookings() throws Exception {
        BookingResponse booking = new BookingResponse();
        booking.setId("b-1");
        booking.setStatus(BookingStatus.PENDING);

        when(bookingService.getAllBookings(any(), anyString(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(booking)));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .param("status", "PENDING")
                        .param("search", "b-")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("b-1"));

        mockMvc.perform(patch("/api/v1/admin/bookings/b-1/approve"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/bookings/b-1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"invalid docs\"}"))
                .andExpect(status().isOk());

        verify(bookingService).approveBooking("b-1");
        verify(bookingService).rejectBooking("b-1", "invalid docs");
    }
}
