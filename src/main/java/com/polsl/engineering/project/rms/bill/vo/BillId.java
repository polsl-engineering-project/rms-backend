package com.polsl.engineering.project.rms.bill.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.UUID;

public record BillId(@JsonValue UUID value) {

    @JsonCreator
    public BillId {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
    }

    public static BillId generate() {
        return new BillId(UUID.randomUUID());
    }

    public static BillId from(String value) {
        return new BillId(UUID.fromString(value));
    }
}
