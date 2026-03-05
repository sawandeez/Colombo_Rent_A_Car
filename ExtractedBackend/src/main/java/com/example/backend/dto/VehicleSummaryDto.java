package com.example.backend.dto;

public class VehicleSummaryDto {
    private String name;
    private String thumbnailUrl;

    public VehicleSummaryDto() {}

    public VehicleSummaryDto(String name, String thumbnailUrl) {
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}
