package com.example.backend.mapper;

import com.example.backend.dto.BookingCreateRequest;
import com.example.backend.dto.BookingResponse;
import com.example.backend.model.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "vehicle", ignore = true)
    BookingResponse toDto(Booking booking);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "bookingTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "advanceAmount", ignore = true)
    @Mapping(target = "advancePaid", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "nicFrontDocumentId", ignore = true)
    @Mapping(target = "drivingLicenseDocumentId", ignore = true)
    @Mapping(target = "startDate", source = "pickupDate")
    @Mapping(target = "endDate", source = "returnDate")
    Booking toEntity(BookingCreateRequest request);
}
