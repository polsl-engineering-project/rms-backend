package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;

import java.time.Instant;

public record OrderApprovedEvent(
        OrderId orderId,
        Instant approvedAt,
        Integer estimatedPreparationMinutes
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return null;
    }

    @Override
    public Instant getOccurredAt() {
        return null;
    }
}
