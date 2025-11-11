package com.polsl.engineering.project.rms.bill.vo;

import java.util.UUID;

public record BillId(UUID value) {
    public static BillId generate() {
        return new BillId(UUID.randomUUID());
    }

    public static BillId from(String value) {
        return new BillId(UUID.fromString(value));
    }
}
