package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;

import java.time.Instant;

public record OrderCompletedEvent(
        OrderId orderId,
        Instant completedAt
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.COMPLETED;
    }

    @Override
    public Instant getOccurredAt() {
        return completedAt();
    }
}
