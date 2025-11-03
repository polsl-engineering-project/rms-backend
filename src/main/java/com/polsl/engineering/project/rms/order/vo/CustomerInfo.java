package com.polsl.engineering.project.rms.order.vo;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public record CustomerInfo(
        String firstName,
        String lastName,
        String phoneNumber
) {
    public CustomerInfo {
        Objects.requireNonNull(firstName, "First name cannot be null");
        Objects.requireNonNull(lastName, "Last name cannot be null");
        Objects.requireNonNull(phoneNumber, "Phone number cannot be null");

        if (firstName.isBlank()) throw new IllegalArgumentException("First name cannot be blank");
        if (lastName.isBlank()) throw new IllegalArgumentException("Last name cannot be blank");
        if (phoneNumber.isBlank()) throw new IllegalArgumentException("Phone number cannot be blank");
    }
}
