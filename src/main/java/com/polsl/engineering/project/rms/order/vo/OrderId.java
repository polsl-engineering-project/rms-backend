package com.polsl.engineering.project.rms.order.vo;

import java.io.Serializable;
import java.util.UUID;

public record OrderId(UUID value) implements Serializable {
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
}
