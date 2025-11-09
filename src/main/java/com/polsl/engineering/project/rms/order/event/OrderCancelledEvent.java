package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;

import java.time.Instant;

public record OrderCancelledEvent(
        OrderId orderId,
        Instant cancelledAt,
        String reason
) implements OrderEvent {
}

