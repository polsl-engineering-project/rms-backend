package com.polsl.engineering.project.rms.order.vo;

import com.polsl.engineering.project.rms.common.exception.InvalidUUIDFormatException;

import java.io.Serializable;
import java.util.UUID;

public record OrderId(UUID value) implements Serializable {
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
    public static OrderId from(String id) {
        try {
            return new OrderId(UUID.fromString(id));
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDFormatException(id);
        }
    }
}
