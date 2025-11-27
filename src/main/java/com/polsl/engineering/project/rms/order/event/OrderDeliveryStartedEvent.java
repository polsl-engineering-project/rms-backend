package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;

import java.time.Instant;

public record OrderDeliveryStartedEvent(
        OrderId orderId,
        Instant startedAt
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.DELIVERY_STARTED;
    }

    @Override
    public Instant getOccurredAt() {
        return startedAt;
    }
}
