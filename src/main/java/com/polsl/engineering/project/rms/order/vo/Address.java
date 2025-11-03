package com.polsl.engineering.project.rms.order.vo;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public record Address(
        String street,
        String houseNumber,
        String apartmentNumber, // nullable dla domów
        String city,
        String postalCode
) {
    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(houseNumber, "House number cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
    }

    public boolean isApartment() {
        return apartmentNumber != null && !apartmentNumber.isBlank();
    }
}
