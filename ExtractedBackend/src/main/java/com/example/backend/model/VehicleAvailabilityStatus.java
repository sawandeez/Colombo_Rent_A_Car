package com.example.backend.model;

public enum VehicleAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    UNDER_MAINTENANCE,
    ADMIN_HELD;

    public static VehicleAvailabilityStatus fromFlags(boolean isAvailable, boolean isUnderMaintenance, boolean isAdminHeld) {
        if (isAdminHeld) {
            return ADMIN_HELD;
        }
        if (isUnderMaintenance) {
            return UNDER_MAINTENANCE;
        }
        if (isAvailable) {
            return AVAILABLE;
        }
        return UNAVAILABLE;
    }
}
