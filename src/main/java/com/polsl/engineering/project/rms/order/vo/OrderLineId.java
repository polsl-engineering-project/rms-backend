package com.polsl.engineering.project.rms.order.vo;

import java.util.UUID;

record OrderLineId(UUID value) {
    static OrderLineId generate() {
        return new OrderLineId(UUID.randomUUID());
    }
}
